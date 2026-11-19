import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import AsyncStorage from '@react-native-async-storage/async-storage';
import FilePickerModule from '../src/utils/FilePickerModule';
import PinScreen from '../src/screens/PinScreen';
import { hasSettingsAccess, revokeSettingsAccess } from '../src/utils/authState';

jest.mock('../src/components/PinInput', () => 'MockPinInput');
jest.mock('../src/utils/FilePickerModule', () => ({ __esModule: true, default: { pickJsonFile: jest.fn() } }));
jest.mock('../src/utils/secureStorage', () => ({ hasSecurePin: jest.fn(async () => true), migrateOldPin: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
const navigation = { navigate: jest.fn(), reset: jest.fn() };
let tree: TestRenderer.ReactTestRenderer;
const file = (name: string, url: string) => ({ name, size: 100, content: JSON.stringify({ version: '1.0', settings: { '@kiosk_url': url } }) });
const passwordForms = () => tree.root.findAll(node => node.type === ('MockPinInput' as any));
const choose = async () => { await act(async () => { await tree.root.findByProps({ testID: 'select-configuration' }).props.onPress(); }); };
beforeEach(async () => {
  jest.clearAllMocks();
  revokeSettingsAccess();
  await AsyncStorage.clear();
  await act(async () => { tree = TestRenderer.create(<PinScreen navigation={navigation as any} />); });
});
afterEach(async () => { await act(async () => { tree.unmount(); }); });

test('the default screen offers import and hides the password form', () => {
  expect(tree.root.findByProps({ testID: 'select-configuration' })).toBeDefined();
  expect(passwordForms()).toHaveLength(0);
});

test('Administration explicitly opens the password form', async () => {
  await act(async () => { tree.root.findByProps({ testID: 'open-administration' }).props.onPress(); });
  expect(passwordForms()).toHaveLength(1);
  await act(async () => { await passwordForms()[0].props.onSuccess(); });
  expect(navigation.navigate).toHaveBeenCalledWith('Settings', undefined);
  expect(navigation.reset).not.toHaveBeenCalled();
});

test('file selection previews first; only successful verification applies and enters kiosk', async () => {
  (FilePickerModule.pickJsonFile as jest.Mock).mockResolvedValueOnce(file('EasyKiosk-backup.json', 'https://example.com'));
  await choose();
  expect(tree.root.findByProps({ testID: 'configuration-preview' })).toBeDefined();
  expect(passwordForms()[0].props.submitLabel).toBe('configurationEntry.confirmImport');
  expect(await AsyncStorage.getItem('@kiosk_url')).toBeNull();
  await act(async () => { await passwordForms()[0].props.onSuccess(); });
  expect(await AsyncStorage.getItem('@kiosk_url')).toBe('https://example.com');
  expect(navigation.reset).toHaveBeenCalledWith({ index: 0, routes: [{ name: 'Kiosk' }] });
  expect(hasSettingsAccess()).toBe(false);
});

test('verification for a replaced file cannot apply the old or the new file', async () => {
  (FilePickerModule.pickJsonFile as jest.Mock).mockResolvedValueOnce(file('a.json', 'https://a.example'));
  await choose();
  const staleVerification = passwordForms()[0].props.onSuccess;
  (FilePickerModule.pickJsonFile as jest.Mock).mockResolvedValueOnce(file('b.json', 'https://b.example'));
  await choose();
  await act(async () => { await staleVerification(); });
  expect(await AsyncStorage.getItem('@kiosk_url')).toBeNull();
  expect(navigation.reset).not.toHaveBeenCalled();
});
