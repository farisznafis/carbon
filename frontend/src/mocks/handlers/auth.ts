import { http } from 'msw';
import { COMPANY, findSite, findUser, operators, users } from '../db';
import { createMockToken, nextId, ok, readJson, readMockSession } from '../utils';

/** Any password works in the demo; the identifier decides which demo account signs in. */
function pickDemoUser(identifier: string) {
  const normalized = identifier.trim().toLowerCase();
  return (
    users.find((user) => user.email === normalized || user.username === normalized) ??
    (normalized.includes('user') ? users[1] : users[0])
  );
}

function profileOf(userId: string | undefined) {
  const user = findUser(userId) ?? users[0];
  return {
    companyId: 1,
    username: user.username,
    role: user.role,
    firstName: user.firstName,
    lastName: user.lastName,
    firstNameKana: user.firstNameKana,
    lastNameKana: user.lastNameKana,
    email: user.email,
    userPosition: user.position,
    phoneNumber: user.phoneNumber,
    siteId: user.siteId,
    siteName: findSite(user.siteId)?.siteName ?? null,
    company: {
      phoneNumber: COMPANY.phoneNumber,
      nameJA: COMPANY.nameJA,
      nameFurigana: COMPANY.nameFurigana,
      nameEN: COMPANY.nameEN,
      address: COMPANY.address,
      corpNumber: COMPANY.corpNumber,
      fax: COMPANY.fax,
      email: COMPANY.email,
    },
  };
}

export const authHandlers = [
  http.post('/api/auth/user/login', async ({ request }) => {
    const body = await readJson<{ identifier: string }>(request);
    const user = pickDemoUser(body.identifier ?? '');
    return ok({
      role: user.role,
      token: createMockToken({ userId: user.id, role: user.role, companyId: COMPANY.companyId }),
      user: { userId: user.id, email: user.email, username: user.username },
      companyName: COMPANY.nameJA,
      siteId: user.siteId,
    });
  }),

  http.post('/api/auth/admin/login', async ({ request }) => {
    const body = await readJson<{ identifier: string }>(request);
    const identifier = (body.identifier ?? '').trim().toLowerCase();
    const operator =
      operators.find((item) => item.username === identifier || item.email === identifier) ?? operators[0];
    return ok({
      role: 'admin',
      token: createMockToken({ userId: String(operator.id), role: 'admin' }),
      admin: {
        operatorId: operator.id,
        email: operator.email,
        username: operator.username,
        name: `${operator.lastName} ${operator.firstName}`,
        role: operator.role,
        companyId: operator.companyId,
      },
      companyName: 'カーボンオペレーションズ株式会社',
    });
  }),

  http.post('/api/logout', () => ok({})),

  http.get('/api/auth/user/profile', ({ request }) =>
    ok({ errors: null, timestamp: new Date().toISOString(), profile: profileOf(readMockSession(request)?.userId) }),
  ),

  http.get('/api/user/profile/:userId', ({ params }) =>
    ok({ errors: null, timestamp: new Date().toISOString(), profile: profileOf(String(params.userId)) }),
  ),

  http.post('/api/auth/user/change-password', () => ok({ message: 'パスワードを変更しました（デモ）' })),
  http.post('/api/auth/password-reset/request', () => ok({ message: 'リセット用メールを送信しました（デモ）' })),
  http.post('/api/auth/action-links/exchange', () =>
    ok({ actionSessionToken: `demo-session-${nextId()}`, purpose: 'PASSWORD_RESET', expiresInSeconds: 900 }),
  ),
  http.post('/api/auth/password-reset/complete', () => ok({ message: 'パスワードを再設定しました（デモ）' })),

  http.get('/api/auth/invitation', () =>
    ok({ email: 'new.member@green-energy.example.jp', companyName: COMPANY.nameJA, siteId: 'S003', siteName: '名古屋物流センター' }),
  ),
  http.post('/api/auth/invitation/complete', () => ok({ message: 'アカウントを登録しました（デモ）' })),

  // Signup and company profile
  http.post('/api/company/signup/company-info/validate', () => ok({ success: true, valid: true })),
  http.post('/api/company/signup/methodology/validate', () => ok({ success: true, valid: true })),
  http.post('/api/company/signup', () =>
    ok({ success: true, message: '登録申請を受け付けました（デモ）', userId: String(nextId()) }),
  ),
  http.get('/api/company/signup/revision', () =>
    ok({
      corporateNumber: COMPANY.corpNumber,
      corporateName: 'さくら食品工業株式会社',
      corporateNameKana: 'サクラショクヒンコウギョウカブシキガイシャ',
      companyNameEnglish: 'Sakura Foods Co., Ltd.',
      corporatePhoneNumber: '0541234567',
      postalCode: '4200001',
      prefecture: '静岡県',
      municipality: '静岡市葵区',
      blocknum: '追手町1-1',
      buildingName: '',
      lastName: '木村',
      firstName: '桜',
      lastNameKana: 'キムラ',
      firstNameKana: 'サクラ',
      position: '総務部長',
      phoneNumber: '09012340000',
      faxNumber: '',
      email: 'kimura@sakura-foods.example.jp',
      methodology: ['EN-R-002'],
      activityInformation: '工場屋根への太陽光発電設備の設置を予定しています。',
      rejectionReason: '法人番号と登記上の名称が一致しません。ご確認のうえ再申請してください。',
    }),
  ),
  http.put('/api/company/signup/revision', () => ok({ message: '再申請を受け付けました（デモ）' })),
  http.patch('/api/company', () => ok({})),
];
