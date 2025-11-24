import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.Queue;

public class UnoModelFlipTest {
    private static class TestUnoModel extends UnoModel {
        private final Queue<Card> queuedRandomCards = new ArrayDeque<>();
        void queueRandomCard(Card c) {
            queuedRandomCards.add(c);
        }
        @Override
        public Card getRandomCard() {
            if (!queuedRandomCards.isEmpty()) {
                return queuedRandomCards.poll();
            }
            return super.getRandomCard();
        }
    }

    @Test
    void testFlipSide() {
        UnoModel model = new UnoModel();
        model.addPlayer("A", false);
        model.addPlayer("B", false);
        Card top = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.THREE
        );
        model.setTopCard(top);
        assertEquals(UnoModel.Side.LIGHT, model.getSide());
        model.flip();
        assertEquals(UnoModel.Side.DARK, model.getSide());
        model.flip();
        assertEquals(UnoModel.Side.LIGHT, model.getSide());
    }

    @Test
    void testDrawFive() {
        UnoModel model = new UnoModel();
        model.addPlayer("A", false);
        model.addPlayer("B", false);
        Player current = model.getCurrPlayer();
        Player next = model.getNextPlayer();
        int before = next.getPersonalDeck().size();
        model.drawFive();
        assertEquals(before + 5, next.getPersonalDeck().size());
        assertSame(current, model.getCurrPlayer());
    }

    @Test
    void testSkipAll() {
        UnoModel model = new UnoModel();
        model.addPlayer("A", false);
        model.addPlayer("B", false);
        model.addPlayer("C", false);
        Player before = model.getCurrPlayer();
        model.skipAll();
        Player after = model.getCurrPlayer();
        assertSame(before, after);
    }

    @Test
    void testWildStack() {
        TestUnoModel model = new TestUnoModel();
        model.addPlayer("A", false);
        model.addPlayer("B", false);
        Card wildStack = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.FOUR,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.WILD_STACK
        );
        model.setTopCard(wildStack);
        model.setInitWildStack(UnoModel.ColoursDark.PINK);
        Player next = model.getNextPlayer();
        int before = next.getPersonalDeck().size();

        Card card1 = new Card(
                UnoModel.Colours.GREEN,
                UnoModel.Values.ONE,
                UnoModel.ColoursDark.TEAL,
                UnoModel.ValuesDark.ONE
        );
        Card card2 = new Card(
                UnoModel.Colours.YELLOW,
                UnoModel.Values.TWO,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.TWO
        );
        Card card3 = new Card(
                UnoModel.Colours.BLUE,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.PINK,
                UnoModel.ValuesDark.FIVE
        );
        model.queueRandomCard(card1);
        model.queueRandomCard(card2);
        model.queueRandomCard(card3);
        assertFalse(model.wildStack());
        assertFalse(model.wildStack());
        assertTrue(model.wildStack());
        assertEquals(before + 3, next.getPersonalDeck().size());
        assertFalse(model.isWildStackCard());
    }

    @Test
    void testLightScoring() {
        UnoModel model = new UnoModel();
        model.addPlayer("Winner");
        model.addPlayer("Loser");
        Player winner = model.getCurrPlayer();
        Player loser = model.getNextPlayer();
        winner.getPersonalDeck().clear();
        loser.getPersonalDeck().clear();
        loser.addCard(new Card(UnoModel.Colours.RED, UnoModel.Values.ONE, UnoModel.ColoursDark.ORANGE, UnoModel.ValuesDark.ONE));
        loser.addCard(new Card(UnoModel.Colours.YELLOW, UnoModel.Values.DRAW_ONE, UnoModel.ColoursDark.PINK, UnoModel.ValuesDark.FIVE));
        loser.addCard(new Card(UnoModel.Colours.GREEN, UnoModel.Values.SKIP, UnoModel.ColoursDark.PURPLE, UnoModel.ValuesDark.FLIP));
        loser.addCard(new Card(UnoModel.Colours.BLUE, UnoModel.Values.WILD, UnoModel.ColoursDark.TEAL, UnoModel.ValuesDark.WILD_STACK));
        loser.addCard(new Card(UnoModel.Colours.RED, UnoModel.Values.WILD_DRAW_TWO, UnoModel.ColoursDark.ORANGE, UnoModel.ValuesDark.DRAW_FIVE));

        int score = model.getScore(winner);
        assertEquals(121, score);
    }

    @Test
    void testDarkScoring() {
        UnoModel model = new UnoModel();
        model.addPlayer("Winner");
        model.addPlayer("Loser");

        Card top = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.THREE
        );
        model.setTopCard(top);
        model.flip();
        Player winner = model.getCurrPlayer();
        Player loser = model.getNextPlayer();
        winner.getPersonalDeck().clear();
        loser.getPersonalDeck().clear();
        loser.addCard(new Card(UnoModel.Colours.RED, UnoModel.Values.ONE, UnoModel.ColoursDark.ORANGE, UnoModel.ValuesDark.ONE));
        loser.addCard(new Card(UnoModel.Colours.YELLOW, UnoModel.Values.TWO, UnoModel.ColoursDark.PINK, UnoModel.ValuesDark.DRAW_FIVE));
        loser.addCard(new Card(UnoModel.Colours.GREEN, UnoModel.Values.THREE, UnoModel.ColoursDark.PURPLE, UnoModel.ValuesDark.SKIP_ALL));
        loser.addCard(new Card(UnoModel.Colours.BLUE, UnoModel.Values.FOUR, UnoModel.ColoursDark.TEAL, UnoModel.ValuesDark.WILD_STACK));
        int score = model.getScore(winner);
        assertEquals(111, score);
    }
}
