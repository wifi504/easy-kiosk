export {};

jest.mock('../src/utils/authPolicy', () => ({
  AUTH_MODE: 'totp', FIXED_PIN_MODE: false, TOTP_ENABLED: true, DEFAULT_PIN: '123456',
  verifyTotp: jest.fn(),
}));
const policy = require('../src/utils/authPolicy');
const storage = require('@react-native-async-storage/async-storage');
const keychain = require('react-native-keychain');
const auth = require('../src/utils/secureStorage');

beforeEach(async () => { await storage.clear(); jest.clearAllMocks(); });

test('TOTP ignores old/default PINs and only calls the compiled policy', async () => {
  await storage.setItem('@kiosk_pin', '123456');
  policy.verifyTotp.mockResolvedValue(false);
  expect((await auth.verifySecurePin('123456')).success).toBe(false);
  expect(keychain.getGenericPassword).not.toHaveBeenCalled();
  policy.verifyTotp.mockResolvedValue(true);
  expect((await auth.verifySecurePin('654321')).success).toBe(true);
});

test('saved or restored passwords cannot replace TOTP authentication', async () => {
  expect(await auth.saveSecurePin('999999')).toBe(false);
  await auth.clearSecurePin();
  expect(await auth.hasSecurePin()).toBe(true);
  expect(keychain.setGenericPassword).not.toHaveBeenCalled();
  expect(keychain.resetGenericPassword).not.toHaveBeenCalled();
});

test('unavailable network time fails closed without consuming an attempt', async () => {
  policy.verifyTotp.mockRejectedValue(new Error('Time unavailable'));
  const result = await auth.verifySecurePin('654321');
  expect(result.success).toBe(false);
  expect(result.attemptsRemaining).toBeUndefined();
  expect(await storage.getItem('@kiosk_pin_attempts')).toBeNull();
});
