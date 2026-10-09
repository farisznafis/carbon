import { HttpResponse, delay, http } from 'msw';
import { MOCK_LATENCY_MS } from '../utils';

type ZipcloudResult = {
  zipcode: string;
  prefcode: string;
  address1: string;
  address2: string;
  address3: string;
  kana1: string;
  kana2: string;
  kana3: string;
};

/** Real addresses for the codes a demo is likely to type, including the seeded companies and sites. */
const KNOWN_ADDRESSES: Record<string, Omit<ZipcloudResult, 'zipcode'>> = {
  '1000005': { prefcode: '13', address1: '東京都', address2: '千代田区', address3: '丸の内', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ﾁﾖﾀﾞｸ', kana3: 'ﾏﾙﾉｳﾁ' },
  '1000001': { prefcode: '13', address1: '東京都', address2: '千代田区', address3: '千代田', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ﾁﾖﾀﾞｸ', kana3: 'ﾁﾖﾀﾞ' },
  '1050011': { prefcode: '13', address1: '東京都', address2: '港区', address3: '芝公園', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ﾐﾅﾄｸ', kana3: 'ｼﾊﾞｺｳｴﾝ' },
  '1600022': { prefcode: '13', address1: '東京都', address2: '新宿区', address3: '新宿', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ｼﾝｼﾞｭｸｸ', kana3: 'ｼﾝｼﾞｭｸ' },
  '1500002': { prefcode: '13', address1: '東京都', address2: '渋谷区', address3: '渋谷', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ｼﾌﾞﾔｸ', kana3: 'ｼﾌﾞﾔ' },
  '2200012': { prefcode: '14', address1: '神奈川県', address2: '横浜市西区', address3: 'みなとみらい', kana1: 'ｶﾅｶﾞﾜｹﾝ', kana2: 'ﾖｺﾊﾏｼﾆｼｸ', kana3: 'ﾐﾅﾄﾐﾗｲ' },
  '3300854': { prefcode: '11', address1: '埼玉県', address2: 'さいたま市大宮区', address3: '桜木町', kana1: 'ｻｲﾀﾏｹﾝ', kana2: 'ｻｲﾀﾏｼｵｵﾐﾔｸ', kana3: 'ｻｸﾗｷﾞﾁｮｳ' },
  '4500002': { prefcode: '23', address1: '愛知県', address2: '名古屋市中村区', address3: '名駅', kana1: 'ｱｲﾁｹﾝ', kana2: 'ﾅｺﾞﾔｼﾅｶﾑﾗｸ', kana3: 'ﾒｲｴｷ' },
  '5300001': { prefcode: '27', address1: '大阪府', address2: '大阪市北区', address3: '梅田', kana1: 'ｵｵｻｶﾌ', kana2: 'ｵｵｻｶｼｷﾀｸ', kana3: 'ｳﾒﾀﾞ' },
  '6008216': { prefcode: '26', address1: '京都府', address2: '京都市下京区', address3: '東塩小路町', kana1: 'ｷｮｳﾄﾌ', kana2: 'ｷｮｳﾄｼｼﾓｷﾞｮｳｸ', kana3: 'ﾋｶﾞｼｼｵｺｳｼﾞﾁｮｳ' },
  '6500001': { prefcode: '28', address1: '兵庫県', address2: '神戸市中央区', address3: '加納町', kana1: 'ﾋｮｳｺﾞｹﾝ', kana2: 'ｺｳﾍﾞｼﾁｭｳｵｳｸ', kana3: 'ｶﾉｳﾁｮｳ' },
  '7300011': { prefcode: '34', address1: '広島県', address2: '広島市中区', address3: '基町', kana1: 'ﾋﾛｼﾏｹﾝ', kana2: 'ﾋﾛｼﾏｼﾅｶｸ', kana3: 'ﾓﾄﾏﾁ' },
  '8120011': { prefcode: '40', address1: '福岡県', address2: '福岡市博多区', address3: '博多駅前', kana1: 'ﾌｸｵｶｹﾝ', kana2: 'ﾌｸｵｶｼﾊｶﾀｸ', kana3: 'ﾊｶﾀｴｷﾏｴ' },
  '9800021': { prefcode: '04', address1: '宮城県', address2: '仙台市青葉区', address3: '中央', kana1: 'ﾐﾔｷﾞｹﾝ', kana2: 'ｾﾝﾀﾞｲｼｱｵﾊﾞｸ', kana3: 'ﾁｭｳｵｳ' },
  '0600001': { prefcode: '01', address1: '北海道', address2: '札幌市中央区', address3: '北一条西', kana1: 'ﾎｯｶｲﾄﾞｳ', kana2: 'ｻｯﾎﾟﾛｼﾁｭｳｵｳｸ', kana3: 'ｷﾀ1ｼﾞｮｳﾆｼ' },
  '9000014': { prefcode: '47', address1: '沖縄県', address2: '那覇市', address3: '松尾', kana1: 'ｵｷﾅﾜｹﾝ', kana2: 'ﾅﾊｼ', kana3: 'ﾏﾂｵ' },
};

/**
 * Any other code still fills the form, with the prefecture its first digit
 * usually belongs to, so a demo never stalls on an unlisted code.
 */
const REGION_BY_FIRST_DIGIT: Record<string, Omit<ZipcloudResult, 'zipcode' | 'address3' | 'kana3'>> = {
  '0': { prefcode: '01', address1: '北海道', address2: '札幌市中央区', kana1: 'ﾎｯｶｲﾄﾞｳ', kana2: 'ｻｯﾎﾟﾛｼﾁｭｳｵｳｸ' },
  '1': { prefcode: '13', address1: '東京都', address2: '千代田区', kana1: 'ﾄｳｷｮｳﾄ', kana2: 'ﾁﾖﾀﾞｸ' },
  '2': { prefcode: '14', address1: '神奈川県', address2: '横浜市中区', kana1: 'ｶﾅｶﾞﾜｹﾝ', kana2: 'ﾖｺﾊﾏｼﾅｶｸ' },
  '3': { prefcode: '11', address1: '埼玉県', address2: 'さいたま市大宮区', kana1: 'ｻｲﾀﾏｹﾝ', kana2: 'ｻｲﾀﾏｼｵｵﾐﾔｸ' },
  '4': { prefcode: '23', address1: '愛知県', address2: '名古屋市中区', kana1: 'ｱｲﾁｹﾝ', kana2: 'ﾅｺﾞﾔｼﾅｶｸ' },
  '5': { prefcode: '27', address1: '大阪府', address2: '大阪市北区', kana1: 'ｵｵｻｶﾌ', kana2: 'ｵｵｻｶｼｷﾀｸ' },
  '6': { prefcode: '26', address1: '京都府', address2: '京都市中京区', kana1: 'ｷｮｳﾄﾌ', kana2: 'ｷｮｳﾄｼﾅｶｷﾞｮｳｸ' },
  '7': { prefcode: '34', address1: '広島県', address2: '広島市中区', kana1: 'ﾋﾛｼﾏｹﾝ', kana2: 'ﾋﾛｼﾏｼﾅｶｸ' },
  '8': { prefcode: '40', address1: '福岡県', address2: '福岡市中央区', kana1: 'ﾌｸｵｶｹﾝ', kana2: 'ﾌｸｵｶｼﾁｭｳｵｳｸ' },
  '9': { prefcode: '04', address1: '宮城県', address2: '仙台市青葉区', kana1: 'ﾐﾔｷﾞｹﾝ', kana2: 'ｾﾝﾀﾞｲｼｱｵﾊﾞｸ' },
};

/** Responds in zipcloud's own envelope, which every caller parses directly. */
async function zipcloud(body: { status: number; message: string | null; results: ZipcloudResult[] | null }) {
  await delay(MOCK_LATENCY_MS);
  return HttpResponse.json(body);
}

export const postalCodeHandlers = [
  http.get('https://zipcloud.ibsnet.co.jp/api/search', ({ request }) => {
    const zipcode = (new URL(request.url).searchParams.get('zipcode') ?? '').replace(/-/g, '');

    if (!zipcode) {
      return zipcloud({ status: 400, message: '必須パラメータが指定されていません。', results: null });
    }
    if (!/^\d{7}$/.test(zipcode)) {
      return zipcloud({ status: 400, message: 'パラメータ「郵便番号」の桁数が不正です。', results: null });
    }

    const known = KNOWN_ADDRESSES[zipcode];
    if (known) {
      return zipcloud({ status: 200, message: null, results: [{ zipcode, ...known }] });
    }

    const region = REGION_BY_FIRST_DIGIT[zipcode[0]];
    return zipcloud({
      status: 200,
      message: null,
      results: [{ zipcode, ...region, address3: 'デモ町', kana3: 'ﾃﾞﾓﾁｮｳ' }],
    });
  }),
];
