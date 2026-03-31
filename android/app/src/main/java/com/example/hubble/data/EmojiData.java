package com.example.hubble.data;

import com.example.hubble.data.model.emoji.EmojiCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmojiData {

    public static final List<EmojiCategory> CATEGORIES = new ArrayList<>();

    // emoji -> short name for search
    public static final Map<String, String> EMOJI_NAMES = new HashMap<>();

    static {
        CATEGORIES.add(new EmojiCategory("Smileys & Emotion", "😀", Arrays.asList(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
                "🫠", "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "☺",
                "😚", "😙", "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗",
                "🫂", "🤭", "🫢", "🫣", "🤫", "🤔", "🫡", "🤐", "🤨", "😐",
                "😑", "😶", "🫥", "😏", "😒", "🙄", "😬", "😮‍💨", "🤥", "🫨",
                "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
                "🤧", "🥵", "🥶", "🥴", "😵", "😵‍💫", "🤯", "🤠", "🥳", "🥸",
                "😎", "🤓", "🧐", "😕", "🫤", "😟", "🙁", "☹", "😮", "😯",
                "😲", "😳", "🥺", "🫹", "😦", "😧", "😨", "😰", "😥", "😢",
                "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤",
                "😡", "😠", "🤬", "😈", "👿", "💀", "☠", "💩", "🤡", "👹",
                "👺", "👻", "👽", "👾", "🤖"
        )));

        CATEGORIES.add(new EmojiCategory("People & Body", "👋", Arrays.asList(
                "👋", "🤚", "🖐", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "🫷",
                "🫸", "👌", "🤌", "🤏", "✌", "🤞", "🫰", "🤟", "🤘", "🤙",
                "👈", "👉", "👆", "🖕", "👇", "☝", "🫵", "👍", "👎", "✊",
                "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏",
                "✍", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻",
                "👃", "🧠", "🦷", "🦴", "👀", "👁", "👅", "👄", "🫦", "👶",
                "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓", "👴",
                "👵", "🙍", "🙎", "🙅", "🙆", "💁", "🙋", "🧏", "🙇", "🤦",
                "🤷", "👮", "🕵", "💂", "🥷", "👷", "🫅", "🤴", "👸", "👰",
                "🤵", "🦸", "🦹", "🧙", "🧚", "🧛", "🧜", "🧝", "🧞", "🧟",
                "🧌", "💆", "💇", "🚶", "🧍", "🧎", "🏃", "💃", "🕺", "🕴",
                "👫", "👬", "👭", "💑", "👪", "🧑‍🤝‍🧑"
        )));

        CATEGORIES.add(new EmojiCategory("Animals & Nature", "🐶", Arrays.asList(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄", "🐨",
                "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊",
                "🐒", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺",
                "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜",
                "🪲", "🦟", "🦗", "🪳", "🕷", "🦂", "🐢", "🐍", "🦎", "🦖",
                "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬",
                "🐳", "🐋", "🦈", "🦭", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧",
                "🦣", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🦬", "🐃",
                "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕",
                "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛", "🐓", "🦃", "🦤", "🦚", "🦜",
                "🦢", "🕊", "🐇", "🦝", "🦨", "🦡", "🦫", "🦦", "🦥", "🐁",
                "🐀", "🐿", "🦔", "🌵", "🎄", "🌲", "🌳", "🌴", "🪵", "🌱",
                "🌿", "☘", "🍀", "🎍", "🪴", "🎋", "🍃", "🍂", "🍁", "🪺",
                "🪹", "🍄", "🌾", "💐", "🌷", "🌹", "🥀", "🪷", "🌺", "🌸",
                "🌼", "🌻", "🌞", "🌝", "🌛", "🌜", "🌚", "🌕", "🌖", "🌗",
                "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "🌟", "⭐", "🌠", "🌌"
        )));

        CATEGORIES.add(new EmojiCategory("Food & Drink", "🍎", Arrays.asList(
                "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈",
                "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬",
                "🥒", "🌶", "🫑", "🧄", "🧅", "🥔", "🍠", "🫚", "🥐", "🥯",
                "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓",
                "🥩", "🍗", "🍖", "🦴", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪",
                "🥙", "🧆", "🌮", "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝",
                "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚",
                "🍘", "🍥", "🥮", "🍢", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬",
                "🍫", "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🧃", "🥤", "🧋",
                "☕", "🍵", "🫖", "🍺", "🍻", "🥂", "🍷", "🫗", "🍸", "🍹",
                "🧉", "🍾", "🧊", "🥄", "🍴", "🍽", "🥢", "🧂"
        )));

        CATEGORIES.add(new EmojiCategory("Travel & Places", "✈", Arrays.asList(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎", "🚓", "🚑", "🚒", "🚐",
                "🛻", "🚚", "🚛", "🚜", "🏍", "🛵", "🛺", "🚲", "🛴", "🛹",
                "🛼", "✈", "🛩", "🚀", "🛸", "🚁", "🛶", "⛵", "🚤", "🛥",
                "🛳", "⛴", "🚢", "🚂", "🚆", "🚇", "🚈", "🚉", "🚊", "🚝",
                "🚞", "🏗", "🌁", "🏘", "🏚", "🏠", "🏡", "🏢", "🏣", "🏤",
                "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰",
                "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩", "🕋", "⛲", "⛺",
                "🏕", "🌄", "🌅", "🌇", "🌆", "🏙", "🌃", "🌌", "🌉", "🗺",
                "🧭", "🌍", "🌎", "🌏", "🌐", "🗾", "🏔", "⛰", "🌋", "🗻",
                "🏞", "🏝", "🏜", "🏖", "🏕", "🌠", "🎇", "🎆", "🌈", "🌊"
        )));

        CATEGORIES.add(new EmojiCategory("Activities", "⚽", Arrays.asList(
                "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
                "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳", "🪁",
                "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛷", "🥌", "🎿", "⛷",
                "🏂", "🪂", "🏋", "🤼", "🤸", "🏄", "🚣", "🧗", "🚵", "🚴",
                "🏊", "🤽", "🧘", "🏇", "🤺", "🤾", "🏌", "🎯", "🎳", "🎮",
                "🕹", "🎲", "♟", "🧩", "🪅", "🪆", "🪄", "🎭", "🎪", "🎨",
                "🖼", "🎬", "🎤", "🎧", "🎼", "🎵", "🎶", "🥁", "🪘", "🎷",
                "🎺", "🪗", "🎸", "🎻", "🪕", "🎹", "🪈", "🎀", "🎁", "🎉",
                "🎊", "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧", "🎗", "🎟",
                "🎫", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖", "🏵", "🎪", "🤹"
        )));

        CATEGORIES.add(new EmojiCategory("Objects", "💻", Arrays.asList(
                "📱", "💻", "⌨", "🖥", "🖨", "🖱", "💾", "💿", "📀", "📷",
                "📸", "📹", "🎥", "📞", "☎", "📟", "📺", "📻", "🎙", "🎚",
                "🎛", "⏱", "⏲", "⏰", "🕰", "⌚", "⏳", "⌛", "📡", "🔋",
                "🪫", "🔌", "💡", "🔦", "🕯", "🪔", "🧱", "🪞", "🛋", "🪑",
                "🚽", "🪠", "🚿", "🛁", "🪤", "🪒", "🧴", "🧹", "🧺", "🧻",
                "🪣", "🧼", "🫧", "🪥", "🧽", "🛒", "🚪", "🪟", "🛏", "📦",
                "📫", "📪", "📬", "📭", "📮", "🗳", "✏", "✒", "🖋", "📝",
                "📁", "📂", "🗂", "🗒", "📅", "📆", "📇", "📈", "📉", "📊",
                "📋", "📌", "📍", "🗺", "🔑", "🗝", "🔒", "🔓", "🔨", "🪓",
                "⛏", "⚒", "🛠", "🗡", "⚔", "🛡", "🔧", "🪛", "🔩", "⚙",
                "🗜", "⚖", "🦯", "🔗", "⛓", "🪝", "🧲", "🪜", "🧰", "🪤",
                "💊", "🩺", "🩻", "🩹", "🩼", "🧬", "🔬", "🔭", "🧪", "🧫",
                "🧲", "🔭", "🪬", "🧿", "💈", "⚗", "🔮", "🪄", "🎩", "🪆"
        )));

        CATEGORIES.add(new EmojiCategory("Symbols", "❤", Arrays.asList(
                "❤", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
                "❤‍🔥", "❤‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟",
                "☮", "✝", "☪", "🕉", "☸", "🪯", "✡", "🔯", "🕎", "☯",
                "☦", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏",
                "♐", "♑", "♒", "♓", "🔀", "🔁", "🔂", "▶", "⏩", "⏭",
                "⏯", "◀", "⏪", "⏮", "🔼", "⏫", "🔽", "⏬", "⏸", "⏹",
                "⏺", "🎦", "🔅", "🔆", "📶", "🚫", "🔞", "📵", "🔕", "🔇",
                "🔈", "🔉", "🔊", "🔔", "🔕", "📣", "📢", "💬", "💭", "🗯",
                "♻", "✅", "❎", "🔱", "📛", "🔰", "💠", "ℹ", "🔙", "🔛",
                "🔜", "🔝", "⚡", "💫", "✨", "🔥", "💥", "❄", "🌊", "💧",
                "💦", "🫧", "🌈", "☀", "⛅", "☁", "🌩", "❓", "❗", "‼",
                "⁉", "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "⚫", "⚪", "🟤"
        )));

        CATEGORIES.add(new EmojiCategory("Flags", "🚩", Arrays.asList(
                "🏳", "🏴", "🏁", "🚩", "🏴‍☠",
                "🇺🇳", "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇩", "🇦🇴", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇺",
                "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪", "🇧🇿", "🇧🇯",
                "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇨🇻",
                "🇰🇭", "🇨🇲", "🇨🇦", "🇨🇫", "🇹🇩", "🇨🇱", "🇨🇳", "🇨🇴", "🇨🇷", "🇭🇷",
                "🇨🇺", "🇨🇾", "🇨🇿", "🇩🇰", "🇩🇯", "🇩🇲", "🇩🇴", "🇪🇨", "🇪🇬", "🇸🇻",
                "🇬🇶", "🇪🇷", "🇪🇪", "🇸🇿", "🇪🇹", "🇫🇯", "🇫🇮", "🇫🇷", "🇬🇦", "🇬🇲",
                "🇬🇪", "🇩🇪", "🇬🇭", "🇬🇷", "🇬🇩", "🇬🇹", "🇬🇳", "🇬🇼", "🇬🇾", "🇭🇹",
                "🇭🇳", "🇭🇰", "🇭🇺", "🇮🇸", "🇮🇳", "🇮🇩", "🇮🇷", "🇮🇶", "🇮🇪", "🇮🇱",
                "🇮🇹", "🇯🇲", "🇯🇵", "🇯🇴", "🇰🇿", "🇰🇪", "🇰🇮", "🇰🇷", "🇰🇼", "🇰🇬",
                "🇱🇦", "🇱🇻", "🇱🇧", "🇱🇸", "🇱🇷", "🇱🇾", "🇱🇮", "🇱🇹", "🇱🇺", "🇲🇬",
                "🇲🇼", "🇲🇾", "🇲🇻", "🇲🇱", "🇲🇹", "🇲🇭", "🇲🇷", "🇲🇺", "🇲🇽", "🇫🇲",
                "🇲🇩", "🇲🇨", "🇲🇳", "🇲🇪", "🇲🇦", "🇲🇿", "🇲🇲", "🇳🇦", "🇳🇷", "🇳🇵",
                "🇳🇱", "🇳🇿", "🇳🇮", "🇳🇪", "🇳🇬", "🇳🇴", "🇴🇲", "🇵🇰", "🇵🇼", "🇵🇸",
                "🇵🇦", "🇵🇬", "🇵🇾", "🇵🇪", "🇵🇭", "🇵🇱", "🇵🇹", "🇵🇷", "🇶🇦", "🇷🇴",
                "🇷🇺", "🇷🇼", "🇸🇦", "🇸🇳", "🇷🇸", "🇸🇬", "🇸🇰", "🇸🇮", "🇸🇴", "🇿🇦",
                "🇸🇸", "🇪🇸", "🇱🇰", "🇸🇩", "🇸🇷", "🇸🇪", "🇨🇭", "🇸🇾", "🇹🇼", "🇹🇯",
                "🇹🇿", "🇹🇭", "🇹🇱", "🇹🇬", "🇹🇴", "🇹🇹", "🇹🇳", "🇹🇷", "🇹🇲", "🇹🇻",
                "🇺🇬", "🇺🇦", "🇦🇪", "🇬🇧", "🇺🇸", "🇺🇾", "🇺🇿", "🇻🇺", "🇻🇦", "🇻🇪",
                "🇻🇳", "🇾🇪", "🇿🇲", "🇿🇼",
                "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"
        )));

        // Emoji name map for search (common emojis)
        String[][] names = {
                {"😀", "grinning face smile happy"},
                {"😃", "grinning face big eyes smile happy"},
                {"😄", "grinning face smiling eyes smile happy"},
                {"😁", "beaming face smile grin"},
                {"😆", "grinning squinting face laugh xd"},
                {"😅", "grinning face sweat nervous"},
                {"🤣", "rolling on floor laughing lol rofl"},
                {"😂", "face with tears of joy lol cry laugh"},
                {"🙂", "slightly smiling face smile"},
                {"😉", "winking face wink"},
                {"😊", "smiling face with smiling eyes blush"},
                {"😇", "smiling face with halo angel innocent"},
                {"🥰", "smiling face with hearts love"},
                {"😍", "smiling face with heart eyes love"},
                {"🤩", "star struck wow amazing"},
                {"😘", "face blowing a kiss love kiss"},
                {"😋", "face savoring food yum delicious"},
                {"😛", "face with tongue"},
                {"😜", "winking face with tongue silly"},
                {"🤪", "zany face crazy silly"},
                {"🤑", "money mouth face rich"},
                {"🤗", "smiling face with open hands hug"},
                {"🤔", "thinking face hmm"},
                {"🤐", "zipper mouth face silent"},
                {"😐", "neutral face meh"},
                {"😑", "expressionless face blank"},
                {"😶", "face without mouth silent"},
                {"😏", "smirking face smirk"},
                {"😒", "unamused face unhappy"},
                {"🙄", "face with rolling eyes eye roll"},
                {"😬", "grimacing face"},
                {"🤥", "lying face pinocchio lie"},
                {"😌", "relieved face"},
                {"😔", "pensive face sad"},
                {"😴", "sleeping face zzz sleep"},
                {"🤤", "drooling face"},
                {"😷", "face with medical mask sick"},
                {"🤒", "face with thermometer sick fever"},
                {"🤕", "face with head bandage hurt"},
                {"🤢", "nauseated face sick"},
                {"🤮", "face vomiting sick vomit"},
                {"🤧", "sneezing face cold"},
                {"🥵", "hot face fire"},
                {"🥶", "cold face frozen"},
                {"🥴", "woozy face dizzy drunk"},
                {"😵", "dizzy face"},
                {"🤯", "exploding head mind blown wow"},
                {"🤠", "cowboy hat face western"},
                {"🥳", "partying face celebrate party"},
                {"😎", "smiling face with sunglasses cool"},
                {"🤓", "nerd face glasses"},
                {"🧐", "face with monocle"},
                {"😕", "confused face"},
                {"😟", "worried face"},
                {"😮", "face with open mouth surprised"},
                {"😲", "astonished face wow shocked"},
                {"😳", "flushed face embarrassed"},
                {"🥺", "pleading face puppy eyes"},
                {"😦", "frowning face with open mouth"},
                {"😧", "anguished face"},
                {"😨", "fearful face scared"},
                {"😰", "anxious face with sweat nervous"},
                {"😥", "sad but relieved face"},
                {"😢", "crying face sad tears"},
                {"😭", "loudly crying face sob cry tears"},
                {"😱", "face screaming in fear scared horror"},
                {"😖", "confounded face"},
                {"😣", "persevering face"},
                {"😞", "disappointed face sad"},
                {"😩", "weary face tired"},
                {"😫", "tired face exhausted"},
                {"🥱", "yawning face tired bored"},
                {"😤", "face with steam from nose angry frustrated"},
                {"😡", "pouting face angry mad"},
                {"😠", "angry face mad"},
                {"🤬", "face with symbols on mouth cursing angry"},
                {"😈", "smiling face with horns devil evil"},
                {"👿", "angry face with horns devil"},
                {"💀", "skull death dead"},
                {"☠", "skull and crossbones danger"},
                {"💩", "pile of poo poop"},
                {"🤡", "clown face"},
                {"👹", "ogre monster"},
                {"👺", "goblin"},
                {"👻", "ghost spooky halloween"},
                {"👽", "alien extraterrestrial"},
                {"👾", "alien monster game"},
                {"🤖", "robot face"},
                {"👍", "thumbs up like good"},
                {"👎", "thumbs down dislike bad"},
                {"❤", "red heart love"},
                {"🧡", "orange heart love"},
                {"💛", "yellow heart love"},
                {"💚", "green heart love"},
                {"💙", "blue heart love"},
                {"💜", "purple heart love"},
                {"🖤", "black heart"},
                {"🤍", "white heart"},
                {"💔", "broken heart sad"},
                {"💕", "two hearts love"},
                {"💞", "revolving hearts love"},
                {"💗", "growing heart love"},
                {"💖", "sparkling heart love"},
                {"💯", "hundred points perfect score"},
                {"🔥", "fire hot flame"},
                {"⭐", "star"},
                {"🌟", "glowing star"},
                {"✨", "sparkles"},
                {"💫", "dizzy star"},
                {"💥", "collision bang explosion"},
                {"❄", "snowflake cold winter"},
                {"🌈", "rainbow colorful"},
                {"☀", "sun sunny"},
                {"⚡", "lightning bolt thunder"},
                {"🎉", "party popper celebrate"},
                {"🎊", "confetti ball party"},
                {"🎁", "wrapped gift present birthday"},
                {"🎂", "birthday cake"},
                {"🍕", "pizza"},
                {"🍔", "hamburger burger"},
                {"🍟", "french fries"},
                {"🍦", "soft ice cream"},
                {"🍰", "shortcake cake"},
                {"☕", "hot beverage coffee tea"},
                {"🍺", "beer mug"},
                {"🍻", "clinking beer mugs cheers"},
                {"🥂", "clinking glasses toast champagne"},
                {"🍷", "wine glass"},
                {"⚽", "soccer ball football"},
                {"🏀", "basketball"},
                {"🏈", "american football"},
                {"⚾", "baseball"},
                {"🎾", "tennis"},
                {"🏆", "trophy winner"},
                {"🥇", "first place gold medal"},
                {"🎮", "video game controller gaming"},
                {"🎲", "game die dice"},
                {"♟", "chess pawn"},
                {"📱", "mobile phone smartphone"},
                {"💻", "laptop computer"},
                {"⌨", "keyboard"},
                {"🖥", "desktop computer"},
                {"📷", "camera photo"},
                {"🎵", "musical note music"},
                {"🎶", "musical notes music"},
                {"🎸", "guitar music rock"},
                {"🎹", "musical keyboard piano"},
                {"🥁", "drum music"},
                {"✈", "airplane flight travel"},
                {"🚗", "car automobile travel"},
                {"🚀", "rocket space"},
                {"⛵", "sailboat boat"},
                {"🌍", "earth globe africa europe"},
                {"🌎", "earth globe americas"},
                {"🌏", "earth globe asia"},
                {"🏠", "house home"},
                {"🏢", "office building"},
                {"🏰", "castle"},
                {"🗼", "tokyo tower"},
                {"🗽", "statue of liberty usa"},
                {"⛩", "shinto shrine japan"},
                {"🕌", "mosque"},
                {"⛪", "church"},
                {"🔑", "key"},
                {"🔒", "locked secure"},
                {"🔓", "unlocked"},
                {"🔔", "bell notification"},
                {"💬", "speech bubble chat"},
                {"💭", "thought bubble thinking"},
                {"📣", "megaphone announce"},
                {"📢", "loudspeaker announce"},
                {"✅", "check mark button done ok"},
                {"❎", "cross mark"},
                {"❓", "question mark"},
                {"❗", "exclamation mark"},
                {"🔴", "red circle"},
                {"🟢", "green circle"},
                {"🔵", "blue circle"},
                {"⚫", "black circle"},
                {"⚪", "white circle"},
                {"🚫", "prohibited no ban"},
                {"♻", "recycling symbol"},
                {"🇺🇸", "united states american flag usa"},
                {"🇬🇧", "united kingdom uk british flag"},
                {"🇨🇳", "china chinese flag"},
                {"🇯🇵", "japan japanese flag"},
                {"🇰🇷", "south korea korean flag"},
                {"🇩🇪", "germany german flag"},
                {"🇫🇷", "france french flag"},
                {"🇧🇷", "brazil brazilian flag"},
                {"🇮🇳", "india indian flag"},
                {"🇻🇳", "vietnam vietnamese flag"},
        };

        for (String[] entry : names) {
            EMOJI_NAMES.put(entry[0], entry[1]);
        }
    }

    public static List<String> search(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim().toLowerCase();
        List<String> results = new ArrayList<>();
        for (EmojiCategory category : CATEGORIES) {
            for (String emoji : category.emojis) {
                String name = EMOJI_NAMES.get(emoji);
                if (name != null && name.contains(q)) {
                    results.add(emoji);
                } else if (category.name.toLowerCase().contains(q)) {
                    if (!results.contains(emoji)) results.add(emoji);
                }
            }
        }
        return results;
    }
}
