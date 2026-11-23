import java.util.ArrayList;
import java.util.List;

/** A shared trip: name, dates, invite code, members and chat history. */
public class TripGroup {
    private final String name;
    private final String dates;
    private final String inviteCode;
    private final List<Member> members = new ArrayList<>();
    private final List<ChatMessage> messages = new ArrayList<>();

    public TripGroup(String name, String dates, String inviteCode) {
        this.name = name;
        this.dates = dates;
        this.inviteCode = inviteCode;
    }

    public String getName()               { return name; }
    public String getDates()              { return dates; }
    public String getInviteCode()         { return inviteCode; }
    public List<Member> getMembers()      { return members; }
    public List<ChatMessage> getMessages(){ return messages; }
}
