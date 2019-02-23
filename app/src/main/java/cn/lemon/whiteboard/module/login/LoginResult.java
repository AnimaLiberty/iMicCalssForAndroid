package cn.lemon.whiteboard.module.login;

public class LoginResult {

    private boolean successStatus;
    private String errorMsg;

    public boolean isSuccessStatus() {
        return successStatus;
    }

    public void setSuccessStatus(boolean successStatus) {
        this.successStatus = successStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
