// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function LoginDialog(parent, domain)
{
  if (arguments.length == 0) {
    return;
  }

  var className = null; // XXX

  DwtDialog.call(this, parent, className, "Login");

  this._panel = new LoginPanel(this, domain);
  this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

  this.setView(this._panel);
  this.setTabOrder(this._panel._fields);
};

LoginDialog.prototype = new DwtDialog();
LoginDialog.prototype.constructor = LoginDialog;

// public methods -------------------------------------------------------------

LoginDialog.prototype.println = function()
{
  return "LoginDialog";
};

LoginDialog.prototype.getUser = function()
{
  return this._panel.getUser();
};

LoginDialog.prototype.getPassword = function()
{
  return this._panel.getPassword();
};

LoginDialog.prototype.getDomain = function()
{
  return this._panel.getDomain();
};

LoginDialog.prototype.reportFailure = function(msg)
{
  this._panel.reportFailure(msg);
};

// internal methods -----------------------------------------------------------

LoginDialog.prototype._focusListener = function(ev)
{
  this._panel.focus();
};

// ----------------------------------------------------------------------------
// Login Panel
// ----------------------------------------------------------------------------

function LoginPanel(parent, domain)
{
  if (0 == arguments.length) {
    return;
  }

  this.domain = domain;

  DwtComposite.call(this, parent);

  this._init();
};

LoginPanel.prototype = new DwtComposite();
LoginPanel.prototype.constructor = LoginPanel;

// public methods -------------------------------------------------------------

LoginPanel.prototype.println = function()
{
  return "LoginPanel";
};

LoginPanel.prototype.getDomain = function()
{
  return this.domain;
};

LoginPanel.prototype.getUser = function()
{
  return this._userField.getValue();
};

LoginPanel.prototype.getPassword = function()
{
  return this._passwordField.getValue();
};

LoginPanel.prototype.reportFailure = function(msg)
{
  this._showError(msg);
};

LoginPanel.prototype.focus = function()
{
  this._userField.focus();
};

// private methods ------------------------------------------------------------

LoginPanel.prototype._init = function()
{
  var msgId = Dwt.getNextId();
  var userFieldId = Dwt.getNextId();
  var passwordFieldId = Dwt.getNextId();

  var html = [];
  html.push("Authenticate for domain: ");
  html.push(this.domain);
  html.push("<br/>");
  html.push("<div id='");
  html.push(msgId);
  html.push("'/>");

  html.push("<table border=0>");
  html.push("<tr>");
  html.push("<td>Username:</td>");
  html.push("<td><div id='");
  html.push(userFieldId);
  html.push("'/></td>");
  html.push("</tr>");

  html.push("<tr>");
  html.push("<td>Password:</td>");
  html.push("<td><div id='");
  html.push(passwordFieldId);
  html.push("'/></td>");
  html.push("</tr>");

  html.push("</table>");
  this.getHtmlElement().innerHTML = html.join("");

  this._fields = new Array();

  this._msgLabel = new DwtLabel(this);
  this._msgLabel.reparentHtmlElement(msgId);

  this._userField = new DwtInputField({ parent: this });
  this._userField.reparentHtmlElement(userFieldId);
  this._fields.push(this._userField);

  this._passwordField = new DwtInputField({ parent: this,
        type: DwtInputField.PASSWORD });
  this._passwordField.reparentHtmlElement(passwordFieldId);
  this._fields.push(this._passwordField);
};

LoginPanel.prototype._showError = function(msg)
{
  this._msgLabel.setText(msg);
  this._msgLabel.setVisible(true);
};
