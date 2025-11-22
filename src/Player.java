import java.util.ArrayList;
import java.util.List;

/**
 * Player entity. Holds a name, type (human or AI) and the player's hand.
 */
public class Player {

    public enum Type {
        HUMAN,
        AI
    }

    private final List<Card> personalDeck = new ArrayList<>();
    private final String name;
    private final Type type;

    /** Creates a human player with the given display name. */
    public Player(String name) {
        this(name, Type.HUMAN);
    }

    /** Creates a player with an explicit type. */
    public Player(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /** Live list of cards held by this player. */
    public List<Card> getPersonalDeck() {
        return personalDeck;
    }

    /** Adds a single card to the player's hand. */
    public void addCard(Card c) {
        if (c != null) {
            personalDeck.add(c);
        }
    }

    /** Removes a card from the player's hand. */
    public void removeCard(Card c) {
        personalDeck.remove(c);
    }

    /** Returns true if this player has no cards left. */
    public boolean hasNoCards() {
        return personalDeck.isEmpty();
    }

    /** Player display name. */
    public String getName() {
        return name;
    }

    /** Player type (HUMAN or AI). */
    public Type getType() {
        return type;
    }

    /** Convenience: is this player controlled by the AI. */
    public boolean isAI() {
        return type == Type.AI;
    }
}
