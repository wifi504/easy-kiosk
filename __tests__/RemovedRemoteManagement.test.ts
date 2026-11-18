import AsyncStorage from '@react-native-async-storage/async-storage';
import { StorageService } from '../src/utils/storage';

describe('已移除的远程管理配置', () => {
  beforeEach(async () => { await AsyncStorage.clear(); });

  it('旧配置导入不能重新启用远程服务，也不会导出旧的服务参数', async () => {
    await StorageService.importConfig({
      general: { url: 'https://example.com/kiosk' },
      advanced: {
        restApi: { enabled: true, port: 8080 },
        mqtt: { enabled: true, brokerUrl: 'broker.invalid', username: 'old-user' },
        cameraStream: { enabled: true },
      },
    });
    const saved = await AsyncStorage.getAllKeys();
    expect(saved.some(key => /mqtt|rest_api|camera_stream|cloud/i.test(key))).toBe(false);
    const config = await StorageService.exportConfig();
    expect(config.advanced).not.toHaveProperty('restApi');
    expect(config.advanced).not.toHaveProperty('mqtt');
    expect(config.advanced).not.toHaveProperty('cameraStream');
    expect(config.general).toMatchObject({ url: 'https://example.com/kiosk' });
  });

  it('读取设备上的遗留键不会把它们重新加入导出配置', async () => {
    await AsyncStorage.multiSet([
      ['@kiosk_rest_api_enabled', 'true'],
      ['@kiosk_mqtt_enabled', 'true'],
      ['@cloud_raw_config', JSON.stringify({ advanced: { mqtt: { enabled: true } } })],
    ]);
    const exported = JSON.stringify(await StorageService.exportConfig());
    expect(exported).not.toMatch(/restApi|mqtt|cameraStream|brokerUrl/);
  });
});
