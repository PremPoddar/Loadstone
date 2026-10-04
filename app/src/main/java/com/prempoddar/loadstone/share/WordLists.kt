package com.prempoddar.loadstone.share

/**
 * The vocabularies a pin's location is spelled out in.
 *
 * Each list holds exactly 128 words, so a word carries exactly seven bits and the maths in
 * [WordCode] stays honest. Adding, removing or reordering a word silently changes what every
 * existing code means, so these lists are frozen: a sentence written on a napkin last year has to
 * still find the same bench today.
 *
 * The words were chosen to be sayable down a phone line and hard to confuse with each other, and
 * to be funny in every combination rather than in a lucky few — which is really a property of the
 * grammar, since any adjective here sits happily on any noun here.
 */


/** Slot 1 and slot 5. */
internal val ADJECTIVES: List<String> = listOf(
    "grumpy", "soggy", "bashful", "fluffy", "greasy", "wobbly",
    "smug", "crusty", "dizzy", "flustered", "hefty", "itchy",
    "jolly", "kooky", "lanky", "mopey", "nervous", "oily",
    "plucky", "quirky", "rusty", "sleepy", "tangled", "uppity",
    "velvety", "waxy", "zesty", "antsy", "bulbous", "chunky",
    "drowsy", "eager", "frosty", "gloomy", "husky", "inky",
    "jumpy", "knobbly", "lumpy", "mushy", "noisy", "perky",
    "quaint", "rowdy", "squishy", "tipsy", "unruly", "vain",
    "whiny", "yappy", "zany", "bossy", "clumsy", "damp",
    "elastic", "feisty", "giddy", "haunted", "idle", "jagged",
    "keen", "loyal", "moody", "nosy", "ornery", "peppery",
    "quivering", "ragged", "snoozy", "twitchy", "upbeat", "vacant",
    "weary", "zippy", "brave", "cranky", "dapper", "earnest",
    "fuzzy", "glum", "hairy", "impish", "jittery", "kindly",
    "limp", "meek", "nimble", "obtuse", "pompous", "quizzical",
    "restless", "shifty", "tiny", "unwashed", "vast", "wily",
    "youthful", "zealous", "abrupt", "bitter", "chilly", "dusty",
    "elderly", "forlorn", "gangly", "hollow", "invisible", "jealous",
    "knotted", "livid", "muddy", "naughty", "prickly", "reckless",
    "stubborn", "thirsty", "untidy", "vivid", "wicked", "aloof",
    "bumbling", "confused", "delicate", "exhausted", "frantic", "grubby",
    "heroic", "lumbering",
)

/** Slot 2 and slot 6: always plural, so any verb below agrees with them. */
internal val NOUNS: List<String> = listOf(
    "walruses", "waffles", "penguins", "pickles", "gnomes", "noodles",
    "llamas", "mittens", "otters", "bagels", "hamsters", "trombones",
    "badgers", "pancakes", "ferrets", "kazoos", "wombats", "dumplings",
    "cucumbers", "puffins", "croissants", "yetis", "doorknobs", "beavers",
    "muffins", "parrots", "slippers", "raccoons", "pretzels", "toads",
    "banjos", "turnips", "weasels", "teapots", "lobsters", "bongos",
    "pelicans", "scones", "hedgehogs", "radishes", "sloths", "umbrellas",
    "narwhals", "biscuits", "meerkats", "kettles", "ostriches", "donuts",
    "gerbils", "bagpipes", "armadillos", "crumpets", "tapirs", "lanterns",
    "cabbages", "marmots", "pillows", "mongooses", "tortoises", "accordions",
    "flamingos", "dumbbells", "toucans", "snorkels", "alpacas", "bannisters",
    "quokkas", "trampolines", "cockatoos", "spatulas", "hippos", "casseroles",
    "magpies", "wheelbarrows", "capybaras", "saucepans", "gibbons", "bookmarks",
    "okapis", "harmonicas", "lemurs", "hammocks", "ibexes", "buckets",
    "manatees", "staplers", "chinchillas", "kayaks", "pangolins", "ukuleles",
    "numbats", "sandwiches", "aardvarks", "coathangers", "dingoes", "paperclips",
    "kiwis", "plungers", "skunks", "tambourines", "newts", "omelettes",
    "squirrels", "bathtubs", "stoats", "pumpkins", "vultures", "mousetraps",
    "yaks", "waistcoats", "gophers", "trolleys", "koalas", "galoshes",
    "jackdaws", "crowbars", "salamanders", "onions", "terrapins", "buttons",
    "moles", "kippers", "shrews", "lampshades", "puffballs", "anoraks",
    "oysters", "cutlasses",
)

/** Slot 3 and slot 7. */
internal val ADVERBS: List<String> = listOf(
    "quietly", "loudly", "politely", "backwards", "furiously", "gracefully",
    "hastily", "sideways", "solemnly", "eagerly", "awkwardly", "bravely",
    "cheerfully", "elegantly", "frantically", "gently", "hungrily", "innocently",
    "joyfully", "kindly", "lazily", "merrily", "nervously", "oddly",
    "patiently", "quickly", "rudely", "sadly", "tenderly", "upwards",
    "violently", "warmly", "zealously", "abruptly", "boldly", "calmly",
    "deftly", "earnestly", "fiercely", "grimly", "happily", "idly",
    "jauntily", "keenly", "loosely", "meekly", "neatly", "openly",
    "proudly", "quaintly", "rapidly", "slyly", "tightly", "vaguely",
    "wildly", "anxiously", "blindly", "crisply", "dreamily", "fondly",
    "gleefully", "heavily", "instantly", "jerkily", "knowingly", "lightly",
    "mildly", "noisily", "obviously", "painfully", "quizzically", "roughly",
    "smugly", "thoughtfully", "urgently", "vividly", "weakly", "accidentally",
    "bashfully", "cautiously", "dimly", "endlessly", "faintly", "greedily",
    "hopefully", "intently", "jealously", "lavishly", "mysteriously", "nightly",
    "officially", "perfectly", "recklessly", "stubbornly", "tirelessly", "uneasily",
    "warily", "abundantly", "briskly", "cleverly", "doggedly", "evenly",
    "firmly", "glumly", "helplessly", "impatiently", "jointly", "lovingly",
    "sleepily", "outwardly", "promptly", "quaveringly", "regally", "sternly",
    "triumphantly", "unevenly", "valiantly", "wearily", "yearningly", "absently",
    "bluntly", "coyly", "drowsily", "eerily", "fitfully", "glibly",
    "haughtily", "inwardly",
)

/** Slot 4: bare plural form, so it follows a plural subject. */
internal val VERBS: List<String> = listOf(
    "juggle", "wrestle", "befriend", "polish", "interrogate", "tickle",
    "launch", "borrow", "bake", "smuggle", "inspect", "salute",
    "untangle", "chase", "marinate", "applaud", "balance", "confuse",
    "deliver", "flatten", "guard", "hoard", "ignore", "jostle",
    "knit", "misplace", "nibble", "outrun", "pester", "question",
    "rescue", "shuffle", "tame", "unwrap", "vacuum", "weigh",
    "yank", "admire", "bribe", "collect", "dust", "escort",
    "fold", "gather", "impress", "judge", "lasso", "measure",
    "nudge", "paint", "quiz", "rearrange", "scrub", "trade",
    "unload", "varnish", "barter", "catapult", "dodge", "ferry",
    "gnaw", "haul", "invent", "knock", "lift", "mock",
    "outwit", "pamper", "pinch", "reheat", "toss", "unbox",
    "veto", "whisk", "boil", "carry", "dangle", "examine",
    "flip", "grill", "heckle", "jiggle", "label", "mend",
    "notice", "pack", "rinse", "stack", "upend", "ambush",
    "bundle", "cherish", "deflate", "enrol", "flavour", "garnish",
    "hoist", "iron", "squash", "lacquer", "mimic", "negotiate",
    "obstruct", "parade", "quibble", "ransack", "serenade", "trample",
    "underline", "wallpaper", "repaint", "shampoo", "accost", "blame",
    "cradle", "dismantle", "elbow", "flatter", "grease", "hypnotise",
    "inherit", "jinx", "knead", "lampoon", "moisten", "outvote",
    "pickle", "ridicule",
)
