package com.gamerecs.back.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringNormalizerTest {

    @Test
    void normalize_shouldReturnNull_whenInputIsNull() {
        assertNull(StringNormalizer.normalize(null));
    }

    @Test
    void normalize_shouldConvertToLowercase() {
        assertEquals("pokemon", StringNormalizer.normalize("POKEMON"));
        assertEquals("zelda", StringNormalizer.normalize("ZeLdA"));
    }

    @Test
    void normalize_shouldRemoveDiacritics() {
        assertEquals("pokemon", StringNormalizer.normalize("Pokémon"));
        assertEquals("anakin", StringNormalizer.normalize("Anäkîñ"));
        assertEquals("garcon", StringNormalizer.normalize("garçon"));
        assertEquals("nino", StringNormalizer.normalize("niño"));
    }

    @Test
    void normalize_shouldRemovePunctuation() {
        assertEquals("assassins creed", StringNormalizer.normalize("Assassin's Creed"));
        assertEquals("final fantasy vii", StringNormalizer.normalize("Final Fantasy VII"));
        assertEquals("mario and luigi", StringNormalizer.normalize("Mario & Luigi"));
        assertEquals("star wars knights of the old republic", 
            StringNormalizer.normalize("Star Wars: Knights of the Old Republic"));
    }

    @Test
    void normalize_shouldReplaceAmpersandWithAnd() {
        assertEquals("mario and luigi", StringNormalizer.normalize("Mario & Luigi"));
        assertEquals("dungeons and dragons", StringNormalizer.normalize("Dungeons & Dragons"));
        assertEquals("rock and roll", StringNormalizer.normalize("Rock & Roll"));
    }

    @Test
    void normalize_shouldHandleMultipleSpaces() {
        assertEquals("the legend of zelda", StringNormalizer.normalize("The   Legend    of   Zelda"));
        assertEquals("gears of war", StringNormalizer.normalize("Gears   of   War"));
    }

    @Test
    void normalize_shouldHandleTrimmingAndEmptyStrings() {
        assertEquals("", StringNormalizer.normalize(""));
        assertEquals("test", StringNormalizer.normalize("  test  "));
        assertEquals("test", StringNormalizer.normalize("  test"));
        assertEquals("test", StringNormalizer.normalize("test  "));
    }

    @ParameterizedTest
    @CsvSource({
        "Pokémon, pokemon",
        "Assassin's Creed, assassins creed",
        "Call of Duty: Modern Warfare, call of duty modern warfare",
        "Star Wars™: Knights of the Old Republic™, star wars knights of the old republic",
        "Baldur's Gate 3, baldurs gate 3",
        "Half-Life 2, half life 2",
        "James Cameron's Avatar™, james camerons avatar",
        "Dungeons & Dragons, dungeons and dragons"
    })
    void normalize_shouldHandleVariousGameTitles(String input, String expected) {
        assertEquals(expected, StringNormalizer.normalize(input));
    }
} 
