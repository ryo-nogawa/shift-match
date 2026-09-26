package com.example.shiftmatch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CSRF 対策用インターセプター。
 *
 * <p>POST /shift へのリクエストに対して、Sec-Fetch-Site と Origin ヘッダを検査し、別オリジンからのリクエストを拒否します。
 */
@Component
public class SameOriginInterceptor implements HandlerInterceptor {

  /**
   * リクエスト前処理。
   *
   * <p>POST /shift の場合、Sec-Fetch-Site または Origin ヘッダを検査します。
   * <ul>
   *   <li>Sec-Fetch-Site ヘッダがある場合は、値が "same-origin" または "none" のときだけ許可
   *   <li>Sec-Fetch-Site がない場合は、Origin ヘッダの host[:port] とリクエストの Host ヘッダを比較して、一致する場合だけ許可
   *   <li>両ヘッダともない場合は許可（非ブラウザ・旧ブラウザ対応）
   * </ul>
   *
   * @param request HTTP リクエスト
   * @param response HTTP レスポンス
   * @param handler ハンドラー
   * @return true: 処理続行、false: 処理中止（403 を返す）
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // POST /shift のリクエストのみを対象
    if ("POST".equals(request.getMethod()) && "/shift".equals(request.getRequestURI())) {
      if (!isSameOrigin(request)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
      }
    }

    return true;
  }

  /**
   * リクエストが同じオリジンからのものか判定します。
   *
   * @param request HTTP リクエスト
   * @return 同じオリジンの場合は true、異なる場合は false
   */
  private boolean isSameOrigin(HttpServletRequest request) {
    String secFetchSite = request.getHeader("Sec-Fetch-Site");

    // Sec-Fetch-Site ヘッダがある場合
    if (secFetchSite != null) {
      return "same-origin".equals(secFetchSite) || "none".equals(secFetchSite);
    }

    // Sec-Fetch-Site がない場合、Origin ヘッダを確認
    String origin = request.getHeader("Origin");
    if (origin != null) {
      String hostHeader = request.getHeader("Host");
      if (hostHeader != null) {
        // Origin ヘッダから host[:port] を抽出
        String originHost = extractHostFromOrigin(origin);
        return originHost != null && originHost.equalsIgnoreCase(hostHeader);
      }
      return false;
    }

    // 両ヘッダともない場合は許可
    return true;
  }

  /**
   * Origin ヘッダから host[:port] を抽出します。
   *
   * <p>例：http://localhost:8080 -> localhost:8080
   *
   * @param origin Origin ヘッダの値
   * @return host[:port]
   */
  private String extractHostFromOrigin(String origin) {
    try {
      java.net.URI uri = new java.net.URI(origin);
      String host = uri.getHost();
      int port = uri.getPort();

      if (port == -1) {
        // ポートが指定されていない場合
        return host;
      } else {
        return host + ":" + port;
      }
    } catch (java.net.URISyntaxException e) {
      return "";
    }
  }
}
