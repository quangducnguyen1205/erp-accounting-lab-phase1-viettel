<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false; section>
  <#if section = "header">
    Master Data Portal
  <#elseif section = "form">
    <div class="mdp-login-shell">
      <section class="mdp-brand-panel" aria-label="Master Data Portal">
        <div class="mdp-brand-mark">MDP</div>
        <h1>Master Data Portal</h1>
        <p class="mdp-subtitle">Cổng quản lý danh mục dùng chung theo tenant</p>
        <div class="mdp-flow">
          <span>Web UI</span>
          <span>Kong Gateway</span>
          <span>Backend services</span>
        </div>
        <p class="mdp-note">Đăng nhập bằng Keycloak local để demo phân quyền, tenant isolation và request flow qua gateway.</p>
      </section>

      <section class="mdp-form-panel" aria-label="Đăng nhập">
        <div class="mdp-form-heading">
          <p class="mdp-eyebrow">Local demo</p>
          <h2>Đăng nhập</h2>
          <p>Nhập tài khoản demo để tiếp tục vào Master Data Portal.</p>
        </div>

        <#if message?has_content>
          <div class="mdp-alert mdp-alert-${message.type}">
            <span>${kcSanitize(message.summary)?no_esc}</span>
          </div>
        </#if>

        <form id="kc-form-login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
          <#if !usernameHidden??>
            <div class="${properties.kcFormGroupClass!} mdp-field">
              <label for="username" class="${properties.kcLabelClass!}">Username</label>
              <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="username" aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
            </div>
          </#if>

          <div class="${properties.kcFormGroupClass!} mdp-field">
            <label for="password" class="${properties.kcLabelClass!}">Password</label>
            <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="current-password" aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
          </div>

          <#if messagesPerField.existsError('username','password')>
            <div class="mdp-field-error" id="input-error">
              ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
            </div>
          </#if>

          <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if> />
            <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} mdp-submit" name="login" id="kc-login" type="submit" value="Đăng nhập" />
          </div>
        </form>

        <div class="mdp-demo-accounts">
          <div>
            <strong>tenant1-user / password</strong>
            <span>Accountant</span>
          </div>
          <div>
            <strong>tenant2-user / password</strong>
            <span>Viewer</span>
          </div>
          <p>Thông tin này chỉ dùng cho lab local. Không dùng credential demo cho môi trường thật.</p>
        </div>
      </section>
    </div>
  </#if>
</@layout.registrationLayout>
