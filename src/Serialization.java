import java.io.Serializable;

public class Serialization implements Serializable {
    public static final long serialVersionUID = 1L;

    public static final int MSG_HEADER_CHAT = 0;
    public static final int MSG_HEADER_PRIVATECHAT = 1;
    public static final int MSG_HEADER_DIEROLL = 2;
    public static final int MSG_HEADER_COINFLIP = 3;
    public static final int MSG_HEADER_WHOISHERE = 4;
    public static final int MSG_HEADER_QUIT = 5;

    private int msgHeader;
    private String msg;

    public Serialization(int msgHeader, String msg) {
        this.msgHeader = msgHeader;
        this.msg = msg;
    }

    public int getMsgHeader() {
        return msgHeader;
    }

    public void setMsgHeader(int msgHeader) {
        this.msgHeader = msgHeader;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Serialization{" +
                "msgHeader=" + msgHeader +
                ", msg='" + msg + '\'' +
                '}';
    }
}