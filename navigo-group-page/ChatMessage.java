import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** One entry in the group chat: plain text, an info card, or a system notice. */
public class ChatMessage {
    public enum Kind { TEXT, ITINERARY_CARD, BILL_CARD, SYSTEM }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Kind kind;
    private final String sender;
    private final boolean mine;
    private final String text;        // TEXT / SYSTEM body, or card title
    private final String detail;      // card subtitle
    private final String targetPage;  // page opened by the card's button
    private final String time;

    private ChatMessage(Kind kind, String sender, boolean mine, String text,
                        String detail, String targetPage, String time) {
        this.kind = kind;
        this.sender = sender;
        this.mine = mine;
        this.text = text;
        this.detail = detail;
        this.targetPage = targetPage;
        this.time = time;
    }

    private static String now() { return LocalTime.now().format(TIME); }

    public static ChatMessage text(String sender, boolean mine, String text) {
        return new ChatMessage(Kind.TEXT, sender, mine, text, null, null, now());
    }

    public static ChatMessage text(String sender, boolean mine, String text, String time) {
        return new ChatMessage(Kind.TEXT, sender, mine, text, null, null, time);
    }

    public static ChatMessage card(Kind kind, String sender, boolean mine,
                                   String title, String detail, String targetPage) {
        return new ChatMessage(kind, sender, mine, title, detail, targetPage, now());
    }

    public static ChatMessage card(Kind kind, String sender, boolean mine,
                                   String title, String detail, String targetPage, String time) {
        return new ChatMessage(kind, sender, mine, title, detail, targetPage, time);
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(Kind.SYSTEM, "", false, text, null, null, now());
    }

    public Kind getKind()          { return kind; }
    public String getSender()      { return sender; }
    public boolean isMine()        { return mine; }
    public String getText()        { return text; }
    public String getDetail()      { return detail; }
    public String getTargetPage()  { return targetPage; }
    public String getTime()        { return time; }
}
