declare module "capacitor-kakao-login-plugin" {
    export interface KakaoLoginResponse {
      accessToken: string;
      expiredAt: string;
      expiresIn: string;
      refreshToken: string;
      idToken?: string;
      refreshTokenExpiredAt: string;
      refreshTokenExpiresIn: string;
      tokenType: string;
    }
  
    export interface KakaoLoginPlugin {
      initForWeb(appKey: string): Promise<void>;
      goLogin(): Promise<KakaoLoginResponse>;
      goLogout(): Promise<void>;
      getUserInfo(): Promise<{ value: any }>;
      sendLinkFeed(options: {
        title: string;
        description: string;
        imageUrl: string;
        imageLinkUrl: string;
        buttonTitle: string;
        imageWidth?: number;
        imageHeight?: number;
      }): Promise<void>;
      talkInChannel(options: { publicId: string }): Promise<any>;
    }
  }
  
  declare module "capacitor-kakao-login-plugin/dist/esm/web" {
    import type { KakaoLoginPlugin } from "capacitor-kakao-login-plugin";
    export class KakaoLoginWeb implements KakaoLoginPlugin {
      initForWeb(appKey: string): Promise<void>;
      goLogin(): Promise<import("capacitor-kakao-login-plugin").KakaoLoginResponse>;
      goLogout(): Promise<void>;
      getUserInfo(): Promise<{ value: any }>;
      sendLinkFeed(options: any): Promise<void>;
      talkInChannel(options: any): Promise<any>;
    }
  }
  