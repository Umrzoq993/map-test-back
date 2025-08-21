package com.agri.mapapp.facility;

public enum FacilityType {
    // 9 ta sahifa uchun kerak bo'lgan turlar
    GREENHOUSE,     // Issiqxona
    POULTRY,        // Tovuqxona
    COWSHED,        // Molxona
    TURKEY,         // Kurkaxona
    SHEEPFOLD,      // Qo'yxona
    WORKSHOP,       // Ishlab chiqarish sexi
    AUX_LAND,       // Yordamchi xo'jalik yerlari
    BORDER_LAND,    // Chegara oldi yerlar
    FISHPOND,       // Baliqchilik ko'llari

    // Legacy / mavjud ma'lumotlarni buzmaslik uchun qoldiramiz
    FISHFARM, STABLE, WAREHOUSE, ORCHARD, FIELD, APIARY
}
