import { HttpResponse, http } from 'msw';

/** Builds a one-page PDF so previews and downloads have something real to show. */
function buildDemoPdf(): Uint8Array {
  const text = 'Carbon Platform - Demo document (no backend)';
  const stream = `BT /F1 18 Tf 60 760 Td (${text}) Tj ET`;
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
    `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ];

  let body = '%PDF-1.4\n';
  const offsets: number[] = [];
  objects.forEach((object, index) => {
    offsets.push(body.length);
    body += `${index + 1} 0 obj\n${object}\nendobj\n`;
  });
  const xrefOffset = body.length;
  body += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  body += offsets.map((offset) => `${String(offset).padStart(10, '0')} 00000 n \n`).join('');
  body += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`;

  return new TextEncoder().encode(body);
}

async function buildDemoWorkbook(fileName: string): Promise<ArrayBuffer> {
  const { default: ExcelJS } = await import('exceljs');
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('デモ');
  sheet.addRow([fileName]);
  sheet.addRow([]);
  sheet.addRow(['年度', '新規会員数', '累計会員数', '削減見込量 (t-CO2)']);
  [2025, 2026, 2027, 2028].forEach((year, index) =>
    sheet.addRow([`${year}年度`, index < 2 ? 4 : 2, Math.min(4 * (index + 1), 12), 180 + index * 15]),
  );
  return workbook.xlsx.writeBuffer() as Promise<ArrayBuffer>;
}

export const fileHandlers = [
  http.get('*/mock-files/:fileName', async ({ params }) => {
    const fileName = decodeURIComponent(String(params.fileName));
    const disposition = `inline; filename*=UTF-8''${encodeURIComponent(fileName)}`;

    if (fileName.toLowerCase().endsWith('.xlsx')) {
      return HttpResponse.arrayBuffer(await buildDemoWorkbook(fileName), {
        headers: {
          'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          'Content-Disposition': disposition,
        },
      });
    }

    return new HttpResponse(buildDemoPdf(), {
      headers: { 'Content-Type': 'application/pdf', 'Content-Disposition': disposition },
    });
  }),
];
