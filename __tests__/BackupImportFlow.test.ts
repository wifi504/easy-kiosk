import AsyncStorage from '@react-native-async-storage/async-storage';
import { buildBackupJson, getBackupPreview, importBackupFromContent, parseBackupContent } from '../src/utils/BackupService';
import UpdateModule from '../src/utils/UpdateModule';

const content = (settings: Record<string, unknown>) => JSON.stringify({ version: '1.0', settings, hasPinConfigured: true });
beforeEach(async () => { await AsyncStorage.clear(); });
afterEach(() => { jest.restoreAllMocks(); });

test('exports use the EasyKiosk name', async () => {
  jest.spyOn(UpdateModule, 'getCurrentVersion').mockResolvedValue({ versionName: 'test', versionCode: 1 });
  expect((await buildBackupJson()).filename).toMatch(/^EasyKiosk-backup-.*\.json$/);
});

test('previewing a legacy backup never writes configuration or exposes URL tokens', async () => {
  const parsed = parseBackupContent(content({ '@kiosk_url': 'https://user:pass@example.com/page?token=secret#fragment' }));
  expect(parsed.success).toBe(true);
  expect(getBackupPreview(parsed.data!).url).toBe('https://••••@example.com/page');
  expect(await AsyncStorage.getItem('@kiosk_url')).toBeNull();
});

test('restore imports settings but preserves current credentials and unrelated settings', async () => {
  await AsyncStorage.multiSet([['@kiosk_pin', 'existing'], ['unrelated', 'keep']]);
  const result = await importBackupFromContent(content({ '@kiosk_url': 'https://example.com', '@kiosk_pin': 'replace', '@kiosk_pin_attempts': '0' }));
  expect(result.success).toBe(true);
  expect(await AsyncStorage.getItem('@kiosk_url')).toBe('https://example.com');
  expect(await AsyncStorage.getItem('@kiosk_pin')).toBe('existing');
  expect(await AsyncStorage.getItem('@kiosk_pin_attempts')).toBeNull();
  expect(await AsyncStorage.getItem('unrelated')).toBe('keep');
});

test('invalid settings are rejected before writing anything', async () => {
  expect(parseBackupContent(content({ '@kiosk_url': { invalid: true } })).success).toBe(false);
  expect(parseBackupContent(content({ '@kiosk_display_mode': 'invalid' })).success).toBe(false);
  expect(parseBackupContent('{"version":"1.0","settings":[]}').success).toBe(false);
  expect((await importBackupFromContent(content({ '@kiosk_pin': 'not-configuration' }))).success).toBe(false);
  expect(await AsyncStorage.getAllKeys()).toEqual([]);
});

test('a partially failed write restores previous values and does not report success', async () => {
  await AsyncStorage.setItem('@kiosk_url', 'https://old.example');
  jest.spyOn(AsyncStorage, 'multiSet').mockImplementationOnce(async pairs => {
    await AsyncStorage.setItem(pairs[0][0], String(pairs[0][1]));
    throw new Error('Storage unavailable');
  });
  const result = await importBackupFromContent(content({ '@kiosk_url': 'https://new.example', '@kiosk_enabled': 'true' }));
  expect(result.success).toBe(false);
  expect(await AsyncStorage.getItem('@kiosk_url')).toBe('https://old.example');
  expect(await AsyncStorage.getItem('@kiosk_enabled')).toBeNull();
});
