public class Member {
    private final String name;
    private final String role;   // "Host" or "Member"
    private final boolean online;

    public Member(String name, String role, boolean online) {
        this.name = name;
        this.role = role;
        this.online = online;
    }

    public String getName()   { return name; }
    public String getRole()   { return role; }
    public boolean isOnline() { return online; }
}
