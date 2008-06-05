package org.alliance.core.settings;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:43:46
 */
public class Server extends SettingClass {
    private Integer port;
    private String hostname;
    private Integer lansupport=0;

    public Server() {
    }

    public Server(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public static int createRandomPort() {
        int p;
        do {
            p = (int)(Math.random()*35000+1500);
        } while(isReservedPort(p));
        return p;
    }

    public static boolean isReservedPort(int port) {
        return Arrays.binarySearch(reservedPorts, port) >= 0;
    }

    public Integer getLansupport() {
        return lansupport;
    }

    public void setLansupport(Integer lansupport) {
        this.lansupport = lansupport;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    private static final int[] reservedPorts = {
            1,
            2,
            3,
            5,
            7,
            11,
            13,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            25,
            27,
            29,
            31,
            33,
            37,
            38,
            39,
            41,
            42,
            43,
            44,
            45,
            46,
            47,
            48,
            49,
            50,
            51,
            52,
            53,
            54,
            55,
            56,
            58,
            61,
            62,
            63,
            64,
            65,
            66,
            67,
            68,
            69,
            70,
            71,
            72,
            73,
            74,
            76,
            78,
            79,
            80,
            81,
            82,
            83,
            84,
            85,
            86,
            88,
            89,
            90,
            91,
            92,
            93,
            94,
            95,
            96,
            97,
            98,
            99,
            101,
            102,
            103,
            104,
            105,
            106,
            107,
            108,
            109,
            110,
            111,
            112,
            113,
            114,
            115,
            116,
            117,
            118,
            119,
            120,
            121,
            122,
            123,
            124,
            125,
            126,
            127,
            128,
            129,
            130,
            131,
            132,
            133,
            134,
            135,
            136,
            137,
            138,
            139,
            140,
            141,
            142,
            143,
            144,
            145,
            146,
            147,
            148,
            149,
            150,
            151,
            152,
            153,
            154,
            155,
            156,
            157,
            158,
            159,
            160,
            161,
            162,
            163,
            164,
            165,
            166,
            167,
            168,
            169,
            170,
            171,
            172,
            173,
            174,
            175,
            176,
            177,
            178,
            179,
            180,
            181,
            182,
            183,
            184,
            185,
            186,
            187,
            188,
            189,
            190,
            191,
            192,
            193,
            194,
            195,
            196,
            197,
            198,
            199,
            200,
            201,
            202,
            203,
            204,
            205,
            206,
            207,
            208,
            209,
            210,
            211,
            212,
            213,
            214,
            215,
            216,
            217,
            218,
            219,
            220,
            221,
            222,
            223,
            242,
            243,
            244,
            245,
            246,
            247,
            248,
            256,
            257,
            258,
            259,
            260,
            261,
            262,
            263,
            280,
            281,
            282,
            309,
            310,
            344,
            345,
            346,
            347,
            348,
            349,
            350,
            351,
            352,
            352,
            354,
            357,
            368,
            371,
            372,
            373,
            374,
            375,
            376,
            377,
            378,
            379,
            380,
            381,
            382,
            383,
            384,
            385,
            386,
            387,
            388,
            389,
            390,
            391,
            392,
            393,
            394,
            395,
            396,
            397,
            398,
            399,
            400,
            401,
            402,
            403,
            404,
            405,
            406,
            407,
            408,
            409,
            410,
            411,
            412,
            413,
            414,
            415,
            416,
            417,
            418,
            419,
            420,
            421,
            422,
            423,
            424,
            425,
            426,
            427,
            428,
            429,
            430,
            431,
            432,
            433,
            434,
            435,
            436,
            437,
            438,
            439,
            440,
            441,
            442,
            443,
            444,
            445,
            446,
            447,
            448,
            449,
            450,
            451,
            452,
            453,
            454,
            455,
            456,
            457,
            458,
            459,
            460,
            461,
            462,
            463,
            464,
            465,
            466,
            467,
            468,
            469,
            470,
            471,
            472,
            473,
            474,
            475,
            476,
            477,
            478,
            479,
            480,
            481,
            482,
            483,
            484,
            485,
            486,
            487,
            488,
            489,
            490,
            491,
            492,
            493,
            494,
            495,
            496,
            497,
            498,
            499,
            500,
            501,
            502,
            503,
            504,
            505,
            506,
            507,
            508,
            509,
            510,
            511,
            512,
            513,
            513,
            514,
            514,
            515,
            516,
            517,
            518,
            519,
            520,
            521,
            522,
            523,
            524,
            525,
            526,
            527,
            528,
            529,
            530,
            531,
            532,
            533,
            534,
            535,
            536,
            537,
            538,
            539,
            540,
            541,
            542,
            543,
            544,
            545,
            546,
            547,
            548,
            549,
            550,
            551,
            552,
            553,
            554,
            555,
            556,
            557,
            558,
            559,
            560,
            561,
            562,
            563,
            564,
            565,
            566,
            567,
            568,
            569,
            570,
            571,
            572,
            573,
            574,
            575,
            576,
            577,
            578,
            579,
            580,
            581,
            582,
            583,
            584,
            585,
            586,
            587,
            588,
            589,
            590,
            591,
            592,
            600,
            606,
            607,
            608,
            609,
            610,
            611,
            612,
            613,
            614,
            615,
            616,
            617,
            618,
            619,
            620,
            621,
            633,
            634,
            635,
            636,
            637,
            666,
            667,
            668,
            669,
            670,
            671,
            672,
            673,
            674,
            675,
            704,
            705,
            709,
            710,
            729,
            730,
            731,
            741,
            742,
            744,
            747,
            748,
            749,
            750,
            799,
            886,
            887,
            888,
            900,
            911,
            989,
            990,
            991,
            992,
            993,
            994,
            995,
            1024,
            1025,
            1027,
            1029,
            1030,
            1031,
            1032,
            1034,
            1035,
            1080,
            1117,
            1140,
            1155,
            1212,
            1214,
            1352,
            1417,
            1418,
            1419,
            1420,
            1433,
            1434,
            1437,
            1451,
            1494,
            1503,
            1504,
            1512,
            1547,
            1559,
            1584,
            1585,
            1611,
            1612,
            1680,
            1731,
            1732,
            1735,
            1745,
            1789,
            1801,
            1863,
            1986,
            1987,
            1988,
            1989,
            1990,
            1991,
            1992,
            1993,
            1994,
            1995,
            1996,
            1997,
            1998,
            1999,
            2000,
            2047,
            2048,
            2049,
            2080,
            2090,
            2090,
            2091,
            2091,
            2095,
            2233,
            2300,
            2346,
            2644,
            2784,
            3000,
            3001,
            3100,
            3230,
            3264,
            3268,
            3269,
            3270,
            3389,
            3453,
            3782,
            3855,
            3999,
            4000,
            4899,
            5000,
            5001,
            5003,
            5190,
            5310,
            5500,
            5631,
            5632,
            5670,
            5800,
            5900,
            6112,
            6346,
            6667,
            6699,
            6700,
            6880,
            6891,
            6901,
            6970,
            7000,
            7013,
            7070,
            7777,
            7875,
            8000,
            8000,
            8010,
            8076,
            8077,
            8080,
            8888,
            9442,
            9999,
            12053,
            12083,
            16000,
            16639,
            20000,
            26000,
            26214,
            27015,
            27660,
            27910,
            28910,
            28910,
            41000,
            47624,
            47624,
            51210
    };

    static {
        Arrays.sort(reservedPorts);
    }
}
