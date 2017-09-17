package com.emc.ldap;

public class PasswordChange {

    String name;
    String password;
    String oldPassword;

    String url;
    String cnsuffix;
    String cnprefix;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCnsuffix() {
        return cnsuffix;
    }

    public void setCnsuffix(String cnsuffix) {
        this.cnsuffix = cnsuffix;
    }

    public String getCnprefix() {
        return cnprefix;
    }

    public void setCnprefix(String cnprefix) {
        this.cnprefix = cnprefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordRepeat() {
        return passwordRepeat;
    }

    public void setPasswordRepeat(String passwordRepeat) {
        this.passwordRepeat = passwordRepeat;
    }

    String passwordRepeat;

}
