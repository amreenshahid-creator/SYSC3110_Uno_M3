import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TestAI {
    @Test
    void testAIFlag() {
        Player human = new Player("Alice", false);
        Player ai = new Player("Bot", true);
        assertFalse(human.isAI());
        assertTrue(ai.isAI());
        assertEquals("Bot", ai.getName());
    }
    @Test
    void testChoosesPlayableCard() {
        UnoModel model = new UnoModel();
        model.addPlayer("AI", true);
        model.addPlayer("Human", false);
        Player aiPlayer = model.getCurrPlayer();
        aiPlayer.getPersonalDeck().clear();
        Card top = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.ONE
        );
        model.setTopCard(top);
        Card redFive = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.FIVE,
                UnoModel.ColoursDark.PINK,
                UnoModel.ValuesDark.FIVE
        );
        Card yellowThree = new Card(
                UnoModel.Colours.YELLOW,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.PURPLE,
                UnoModel.ValuesDark.THREE
        );

        Card greenTwo = new Card(
                UnoModel.Colours.GREEN,
                UnoModel.Values.TWO,
                UnoModel.ColoursDark.TEAL,
                UnoModel.ValuesDark.TWO
        );
        aiPlayer.addCard(redFive);
        aiPlayer.addCard(yellowThree);
        aiPlayer.addCard(greenTwo);
        List<Card> playable = model.getPlayableCards(aiPlayer);
        Card chosen = model.chooseAICardForCurrPlayer();
        assertNotNull(chosen);
        assertTrue(playable.contains(chosen));
        assertNotEquals(greenTwo, chosen);
    }

    @Test
    void testPrefersActionCard() {
        UnoModel model = new UnoModel();
        model.addPlayer("AI", true);
        model.addPlayer("Human", false);
        Player ai = model.getCurrPlayer();
        ai.getPersonalDeck().clear();
        Card top = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.ONE
        );
        model.setTopCard(top);
        Card numberCard = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.FIVE,
                UnoModel.ColoursDark.PINK,
                UnoModel.ValuesDark.FIVE
        );
        Card actionCard = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.DRAW_ONE,
                UnoModel.ColoursDark.PURPLE,
                UnoModel.ValuesDark.DRAW_FIVE
        );
        ai.addCard(numberCard);
        ai.addCard(actionCard);
        Card chosen = model.chooseAICardForCurrPlayer();
        assertNotNull(chosen);
        assertEquals(UnoModel.Values.DRAW_ONE, chosen.getValue());
    }

    @Test
    void testNoPlayableCard() {
        UnoModel model = new UnoModel();
        model.addPlayer("AI", true);
        model.addPlayer("Human", false);
        Player ai = model.getCurrPlayer();
        ai.getPersonalDeck().clear();
        Card top = new Card(
                UnoModel.Colours.RED,
                UnoModel.Values.THREE,
                UnoModel.ColoursDark.ORANGE,
                UnoModel.ValuesDark.ONE
        );
        model.setTopCard(top);
        Card greenOne = new Card(
                UnoModel.Colours.GREEN,
                UnoModel.Values.ONE,
                UnoModel.ColoursDark.TEAL,
                UnoModel.ValuesDark.ONE
        );
        Card blueTwo = new Card(
                UnoModel.Colours.BLUE,
                UnoModel.Values.TWO,
                UnoModel.ColoursDark.PINK,
                UnoModel.ValuesDark.TWO
        );
        ai.addCard(greenOne);
        ai.addCard(blueTwo);
        assertFalse(model.currPlayerHasPlayableCard());
        assertNull(model.chooseAICardForCurrPlayer());
    }
}

