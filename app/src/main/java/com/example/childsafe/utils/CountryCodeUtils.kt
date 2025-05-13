package com.example.childsafe.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Data class representing a country with its name, ISO code and dial code
 */
data class CountryCodeInfo(
    val name: String,
    val code: String,
    val dialCode: String,
    val flagEmoji: String
) {
    fun displayText(): String = "$flagEmoji $dialCode ($name)"
}

/**
 * Utility class for handling country codes using Google's libphonenumber library
 */
object CountryCodeUtils {
    
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    
    /**
     * Get a list of all countries with their dial codes
     * 
     * @return List of CountryCodeInfo objects sorted by country name
     */
    fun getCountryCodes(): List<CountryCodeInfo> {
        val supportedRegions = phoneNumberUtil.supportedRegions
        
        return supportedRegions.map { regionCode ->
            val countryCode = phoneNumberUtil.getCountryCodeForRegion(regionCode)
            val locale = Locale("", regionCode)
            val countryName = locale.displayCountry
            
            CountryCodeInfo(
                name = countryName,
                code = regionCode,
                dialCode = "+$countryCode",
                flagEmoji = getFlagEmojiForCountryCode(regionCode)
            )
        }.sortedBy { it.name }
    }
    
    /**
     * Get the country code for the user's current locale
     * 
     * @return CountryCodeInfo for the detected country, or Vietnam as default
     */
    fun getDefaultCountryCode(): CountryCodeInfo {
        val currentLocale = Locale.getDefault()
        val userCountry = currentLocale.country
        
        return try {
            val countryCode = phoneNumberUtil.getCountryCodeForRegion(userCountry)
            if (countryCode > 0) {
                CountryCodeInfo(
                    name = currentLocale.displayCountry,
                    code = userCountry,
                    dialCode = "+$countryCode",
                    flagEmoji = getFlagEmojiForCountryCode(userCountry)
                )
            } else {
                // Default to Vietnam if we can't detect the country
                getCountryCodes().find { it.code == "VN" }
                    ?: CountryCodeInfo("Vietnam", "VN", "+84", "🇻🇳")
            }
        } catch (e: Exception) {
            // Default to Vietnam as fallback
            CountryCodeInfo("Vietnam", "VN", "+84", "🇻🇳")
        }
    }
    
    /**
     * Format a phone number according to the international standard
     * 
     * @param phoneNumber The phone number to format
     * @param countryCode The ISO country code (e.g., "US", "VN")
     * @return Formatted phone number or original number if formatting fails
     */
    fun formatPhoneNumber(phoneNumber: String, countryCode: String): String {
        return try {
            val number = phoneNumberUtil.parse(phoneNumber, countryCode)
            phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (e: Exception) {
            phoneNumber
        }
    }
    
    /**
     * Generate flag emoji from country code
     * 
     * @param countryCode ISO country code (e.g., "US", "VN")
     * @return Flag emoji string
     */
    private fun getFlagEmojiForCountryCode(countryCode: String): String {
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}
