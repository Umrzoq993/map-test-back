package com.agri.mapapp.facility;

public enum FacilityType {
    // ✅ Biz ishlatadigan turlar (11 ta)
    GREENHOUSE,             // Issiqxona
    POULTRY_MEAT,           // Tovuqxona (go'sht)
    POULTRY_EGG,            // Tovuqxona (tuxum)
    TURKEY,                 // Kurkaxona
    COWSHED,                // Molxona
    SHEEPFOLD,              // Qo'yxona
    WORKSHOP_SAUSAGE,       // Ishlab chiqarish sexi (kolbasa)
    WORKSHOP_COOKIE,        // Ishlab chiqarish sexi (pechenye)
    AUX_LAND,               // Yordamchi xo'jalik yer maydonlari
    BORDER_LAND,            // Chegara oldi yer maydonlari
    FISHPOND,               // Baliqchilik ko'llari

    // 🔶 Legacy — vaqtincha qoldiriladi (EnumType.STRING bo'lsa ordinga ta'sir qilmaydi)
    @Deprecated POULTRY,
    @Deprecated WORKSHOP,
    @Deprecated FISHFARM, @Deprecated STABLE, @Deprecated WAREHOUSE, @Deprecated ORCHARD, @Deprecated FIELD, @Deprecated APIARY
}
