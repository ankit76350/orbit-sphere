package com.orbitastra.backend.models.undone.a_working.mess;

import java.time.LocalTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "mess_meal_types")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMealType extends SchoolBase {

    // Example Database
    
    //! mess_meal_types
    // Breakfast
    // Lunch
    // Evening Snacks
    // Dinner

    // ↓

    //! mess_halls
    // Main Hostel Mess

    // Girls Hostel Mess

    // Staff Mess

    // ↓

    // mess_menus
    // 2027-07-01

    // Breakfast

    // Milk

    // Bread

    // Butter

    // Banana

    // ↓

    // mess_attendance
    // Rahul

    // Breakfast

    // Present

    // 08:12 AM

    // ↓

    // mess_kitchen_items
    // Rice

    // 200 KG

    // ------------

    // Oil

    // 80 Liter

    // ------------

    // Sugar

    // 120 KG

    // ↓

    // mess_kitchen_transactions
    // Purchase

    // Rice

    // +100 KG

    // ------------

    // Consumption

    // Rice

    // -18 KG

    // ------------

    // Wastage

    // Milk

    // -5 Liter

    // BREAKFAST, LUNCH, SNACKS, DINNER
    @Indexed(unique = true)
    private String name;

    // 07:00
    private LocalTime servingFrom;

    // 09:00
    private LocalTime servingTo;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean active = true;
}

// Kitchen receives Rice
// ↓
// MessKitchenTransaction (PURCHASE)
// ↓
// MessKitchenItem.currentQuantity updated

// ----------------------------------------

// Admin creates today's Lunch menu
// ↓
// MessMenu

// ----------------------------------------

// Student scans RFID
// ↓
// MessAttendance

// ----------------------------------------

// Kitchen uses 18 KG Rice
// ↓
// MessKitchenTransaction (CONSUMPTION)
// ↓
// MessKitchenItem.currentQuantity updated
