package com.example.shiftmatch.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC の設定。
 *
 * <p>CSRF 対策用のインターセプターを登録します。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final SameOriginInterceptor sameOriginInterceptor;

  /**
   * コンストラクタです。
   *
   * @param sameOriginInterceptor 同一オリジンチェック用インターセプター
   */
  @Autowired
  public WebConfig(SameOriginInterceptor sameOriginInterceptor) {
    this.sameOriginInterceptor = sameOriginInterceptor;
  }

  /**
   * インターセプターを登録します。
   *
   * @param registry インターセプター登録用オブジェクト
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(sameOriginInterceptor);
  }
}
