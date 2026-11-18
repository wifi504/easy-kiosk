import { NativeModules } from 'react-native';

const { DeviceUtilitiesModule } = NativeModules;
export const deviceUtilities = {
  async getCamera2Devices(): Promise<Array<{ id: string; position: string; maxWidth: number; maxHeight: number }>> {
    return DeviceUtilitiesModule ? DeviceUtilitiesModule.getCamera2Devices() : [];
  },
  async captureCamera2Photo(facing: string, quality: number): Promise<string> {
    if (!DeviceUtilitiesModule) throw new Error('Camera helpers unavailable');
    return DeviceUtilitiesModule.captureCamera2Photo(facing, quality);
  },
};
