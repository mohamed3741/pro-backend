package com.pro.model.Enum;

public enum Weekday {
    MONDAY((short)0),
    TUESDAY((short)1),
    WEDNESDAY((short)2),
    THURSDAY((short)3),
    FRIDAY((short)4),
    SATURDAY((short)5),
    SUNDAY((short)6);

    public final short dbValue;
    Weekday(short dbValue) { this.dbValue = dbValue; }

    public static Weekday fromDb(short v) {
        for (Weekday d : values()) if (d.dbValue == v) return d;
        throw new IllegalArgumentException("Invalid weekday value: " + v);
    }
}


