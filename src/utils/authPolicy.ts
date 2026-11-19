import { NativeModules, Platform } from 'react-native';

const nativePolicy = NativeModules.AuthPolicyModule;
// A missing/broken Android policy must never unlock the fixed-PIN fallback.
export const AUTH_MODE: 'pin' | 'totp' | 'error' = nativePolicy?.mode ?? (Platform.OS === 'android' ? 'error' : 'pin');
export const FIXED_PIN_MODE = AUTH_MODE === 'pin';
export const TOTP_ENABLED = AUTH_MODE === 'totp';
export const DEFAULT_PIN = '123456';

export async function verifyTotp(code: string): Promise<boolean> {
  if (!TOTP_ENABLED || !nativePolicy?.verifyTotp) throw new Error('Authentication policy unavailable');
  return nativePolicy.verifyTotp(code);
}

export async function prepareTotpTime(): Promise<void> {
  if (!TOTP_ENABLED || !nativePolicy?.prepareTime) throw new Error('Authentication policy unavailable');
  await nativePolicy.prepareTime();
}
