import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.crush.aiblackboxreview',
  appName: 'AI블리',
  webDir: 'out',
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true,
    backgroundColor: "#00000000",
    initialFocus: true
  },
  plugins: {
    CapacitorHttp: {
      enabled: true
    }
  }
  // server: {
  //   // url: 'http://192.168.30.192:3000', // 컴퓨터의 로컬 IP 주소
  //   url: "http://10.0.2.2:3000", // 컴퓨터의 로컬 IP 주소
  //   cleartext: true // HTTPS가 아닌 HTTP URL을 사용하는 경우에만 필요
  // }
};

export default config;
