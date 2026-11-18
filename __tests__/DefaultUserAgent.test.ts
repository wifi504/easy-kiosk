/**
 * The user agent a page sees when none is set. A phone one on a tablet or a phone, as it always
 * was; on a TV without the "Mobile" token, so sites serve a layout made for a wide screen.
 */
// A module of its own so the helpers do not collide with another test file's
export {};

const { NativeModules, Platform } = require('react-native');
NativeModules.DeviceUtilitiesModule = {
  executeNativeCommand: jest.fn(),
  captureScreenshotBase64: jest.fn(),
};

NativeModules.EmbeddedChromiumModule = {
  userAgent: 'Mozilla/5.0 (Linux; Android 10; K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/158.0.8127.42 Mobile Safari/537.36',
};

const { defaultUserAgent } = require('../src/components/WebViewComponent');

describe('defaultUserAgent', () => {
  const original = Platform.isTV;
  const setTv = (value: boolean) => {
    Object.defineProperty(Platform, 'isTV', { configurable: true, get: () => value });
  };

  afterEach(() => {
    Object.defineProperty(Platform, 'isTV', { configurable: true, get: () => original });
  });

  it('preserves the actual embedded Chromium version and Android version', () => {
    setTv(false);
    expect(defaultUserAgent()).toBe(
      'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/158.0.8127.42 Mobile Safari/537.36',
    );
  });

  it('drops only the Mobile token on a TV', () => {
    setTv(true);
    expect(defaultUserAgent()).toBe(
      'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/158.0.8127.42 Safari/537.36',
    );
    expect(defaultUserAgent()).not.toContain('Mobile');
  });
});
