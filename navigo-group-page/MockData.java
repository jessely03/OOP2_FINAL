import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sample data so the page works without a backend.
 * Later, replace these methods with real calls (database / server) and keep the UI as is.
 */
public final class MockData {
    private MockData() {}

    public static final String CURRENT_USER = "You";

    private static final Map<String, TripGroup> TRIPS = new LinkedHashMap<>();

    static {
        TripGroup taiwan = new TripGroup("Taiwan Trip", "Dec 20 - Dec 25", "NAVI-TW25");
        taiwan.getMembers().add(new Member("Alice", "Host", true));
        taiwan.getMembers().add(new Member("Daniel", "Member", true));
        taiwan.getMembers().add(new Member(CURRENT_USER, "Member", true));
        taiwan.getMembers().add(new Member("Emma", "Member", false));
        taiwan.getMessages().add(ChatMessage.system("Alice created the trip"));
        taiwan.getMessages().add(ChatMessage.text("Alice", false, "Let's meet at 9am at the airport!", "08:12"));
        taiwan.getMessages().add(ChatMessage.text(CURRENT_USER, true, "Sounds good!", "08:15"));
        taiwan.getMessages().add(ChatMessage.card(ChatMessage.Kind.ITINERARY_CARD, "Daniel", false,
                "Itinerary updated", "Dec 20: Visit Longshan Temple at 1:00 PM", MainFrame.CALENDAR, "08:30"));
        taiwan.getMessages().add(ChatMessage.card(ChatMessage.Kind.BILL_CARD, "Alice", false,
                "New expense added", "Dinner at Taipei 101 - $240 split 4 ways", MainFrame.SPLIT_BILL, "08:41"));
        TRIPS.put(taiwan.getInviteCode(), taiwan);

        TripGroup japan = new TripGroup("Japan Trip", "Mar 3 - Mar 9", "NAVI-JP26");
        japan.getMembers().add(new Member("Kenji", "Host", true));
        japan.getMembers().add(new Member(CURRENT_USER, "Member", true));
        japan.getMembers().add(new Member("Mei", "Member", true));
        japan.getMessages().add(ChatMessage.system("Kenji created the trip"));
        japan.getMessages().add(ChatMessage.text("Kenji", false, "Welcome everyone! Add your ideas for Kyoto.", "20:02"));
        TRIPS.put(japan.getInviteCode(), japan);
    }

    public static TripGroup defaultTrip() {
        return TRIPS.get("NAVI-TW25");
    }

    /** Returns the trip for an invite code, or null if the code is wrong. */
    public static TripGroup findByCode(String code) {
        if (code == null) return null;
        return TRIPS.get(code.trim().toUpperCase());
    }
}
