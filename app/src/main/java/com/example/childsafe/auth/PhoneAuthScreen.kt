package com.example.childsafe.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.childsafe.utils.CountryCodeInfo
import com.example.childsafe.utils.CountryCodeUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

// The CountryCodeInfo class is now imported from the utils package

@Composable
fun PhoneAuthScreen(
    viewModel: PhoneAuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    
    // Country code selection with search - using try-catch to handle any exceptions
    val countryCodeList = remember { 
        try {
            CountryCodeUtils.getCountryCodes()
        } catch (e: Exception) {
            // Fallback to simple list if there's an error
            listOf(
                CountryCodeInfo("Vietnam", "VN", "+84", "🇻🇳"),
                CountryCodeInfo("United States", "US", "+1", "🇺🇸"),
                CountryCodeInfo("United Kingdom", "GB", "+44", "🇬🇧"),
                CountryCodeInfo("Japan", "JP", "+81", "🇯🇵"),
                CountryCodeInfo("China", "CN", "+86", "🇨🇳"),
                CountryCodeInfo("France", "FR", "+33", "🇫🇷"),
                CountryCodeInfo("Australia", "AU", "+61", "🇦🇺"),
                CountryCodeInfo("Germany", "DE", "+49", "🇩🇪")
            )
        }
    }
    var selectedCountry by remember { 
        mutableStateOf(
            try {
                CountryCodeUtils.getDefaultCountryCode()
            } catch (e: Exception) {
                // Vietnam as fallback
                CountryCodeInfo("Vietnam", "VN", "+84", "🇻🇳")
            }
        ) 
    }
    var countrySearchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showCountryCodeDropdown by remember { mutableStateOf(false) }
    var filteredCountries by remember { mutableStateOf(countryCodeList) }
    
    // 6-digit verification code
    var digit1 by remember { mutableStateOf("") }
    var digit2 by remember { mutableStateOf("") }
    var digit3 by remember { mutableStateOf("") }
    var digit4 by remember { mutableStateOf("") }
    var digit5 by remember { mutableStateOf("") }
    var digit6 by remember { mutableStateOf("") }
    
    // Focus request controllers for verification code fields
    val digit1FocusRequester = remember { FocusRequester() }
    val digit2FocusRequester = remember { FocusRequester() }
    val digit3FocusRequester = remember { FocusRequester() }
    val digit4FocusRequester = remember { FocusRequester() }
    val digit5FocusRequester = remember { FocusRequester() }
    val digit6FocusRequester = remember { FocusRequester() }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show error messages in snackbar
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = (authState as AuthState.Error).message
                    )
                }
            }
            is AuthState.Success -> {
                onAuthSuccess()
            }
            else -> {}
        }
    }
    
    // Combine digits to form verification code (6 digits)
    LaunchedEffect(digit1, digit2, digit3, digit4, digit5, digit6) {
        verificationCode = digit1 + digit2 + digit3 + digit4 + digit5 + digit6
        
        // Auto-submit when all 6 digits are entered
        if (verificationCode.length == 6) {
            // Small delay to allow the user to see the completed code
            delay(300)
            viewModel.submitVerificationCode(verificationCode)
        }
    }
    
    // Filter country codes based on search query with improved search algorithm
    LaunchedEffect(countrySearchQuery.text) {
        filteredCountries = if (countrySearchQuery.text.isEmpty()) {
            countryCodeList
        } else {
            val query = countrySearchQuery.text.lowercase().trim()
            
            // First try to find exact matches (for dial code searching)
            val exactMatches = countryCodeList.filter { 
                it.dialCode.contains(query) || 
                it.code.lowercase() == query
            }
            
            // If we have exact matches, prioritize those
            if (exactMatches.isNotEmpty()) {
                exactMatches
            } else {
                // Otherwise do a more fuzzy search
                countryCodeList.filter { country ->
                    country.name.lowercase().contains(query) || 
                    country.dialCode.contains(query) ||
                    country.code.lowercase().contains(query) ||
                    // Also search without "+" for people who type the number without it
                    (query.startsWith("+") && country.dialCode.contains(query)) ||
                    (!query.startsWith("+") && country.dialCode.contains("+$query"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (authState) {
                    is AuthState.Idle, is AuthState.Error -> {
                        // Phone number input step (first two screens from the image)
                        Text(
                            text = "Hãy nhập số điện thoại của bạn!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Số của bạn sẽ chỉ được sử dụng trong những trường hợp khẩn cấp và sẽ không được chuyển tiếp cho bên thứ ba.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country code dropdown selector with fixed size
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(56.dp)
                            ) {
                                Button(
                                    onClick = { showCountryCodeDropdown = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = selectedCountry.flagEmoji,
                                                fontSize = 20.sp
                                            )
                                            Text(
                                                text = selectedCountry.dialCode,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select country code",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Simpler dropdown without nested complex layouts
                                if (showCountryCodeDropdown) {
                                    Popup(
                                        onDismissRequest = { showCountryCodeDropdown = false },
                                        properties = PopupProperties(
                                            focusable = true,
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true
                                        )
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .width(300.dp)
                                                .heightIn(max = 500.dp)
                                                .shadow(elevation = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Header
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Select Country Code",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        
                                        // Search field
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = countrySearchQuery,
                                                onValueChange = { countrySearchQuery = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Search country...") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                        
                                        Divider()
                                        
                                        // Countries list
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(350.dp)
                                        ) {
                                            if (filteredCountries.isEmpty()) {
                                                // Empty state
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("No countries found")
                                                }
                                            } else {
                                                // Simple Column instead of LazyColumn for better stability
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    filteredCountries.forEach { country ->
                                                        val isSelected = country.code == selectedCountry.code
                                                        val backgroundColor = if (isSelected)
                                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                        else
                                                            MaterialTheme.colorScheme.surface
                                                            
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedCountry = country
                                                                    showCountryCodeDropdown = false
                                                                }
                                                                .background(backgroundColor)
                                                                .padding(16.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Flag
                                                            Text(
                                                                text = country.flagEmoji,
                                                                fontSize = 24.sp,
                                                                modifier = Modifier.padding(end = 12.dp)
                                                            )
                                                            
                                                            // Country name
                                                            Column(
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text(
                                                                    text = country.name,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                            
                                                            // Dial code
                                                            Text(
                                                                text = country.dialCode,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        
                                                        Divider()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Phone number field with improved styling
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { 
                                    // Remove any non-digit characters
                                    val filtered = it.filter { char -> char.isDigit() }
                                    phoneNumber = filtered
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { 
                                    Text(
                                        "22 345 6789",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ) 
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(4.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { 
                                // Format phone number with selected country code
                                val formattedNumber = if (phoneNumber.startsWith(selectedCountry.dialCode)) {
                                    phoneNumber
                                } else {
                                    "${selectedCountry.dialCode}$phoneNumber"
                                }
                                viewModel.verifyPhoneNumber(formattedNumber, activity) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = phoneNumber.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Tiếp tục", fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Bằng cách tiếp tục, bạn cho biết bạn đồng ý với điều khoản và Chính sách bảo mật.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    is AuthState.CodeSent -> {
                        // Auto-focus first digit field when verification screen appears
                        LaunchedEffect(Unit) {
                            // Small delay to ensure the UI is ready
                            delay(300)
                            digit1FocusRequester.requestFocus()
                        }
                        
                        // Verification code input step (last two screens from the image)
                        Text(
                            text = "Vui lòng xác nhận số điện thoại của bạn!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Bạn sẽ nhận được mã xác nhận qua tin nhắn SMS tới số điện thoại của mình, vui lòng nhập mã bên dưới để tiếp tục:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Visual code input with 6 separate fields for 6-digit verification in a single row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // First digit
                            OutlinedTextField(
                                value = digit1,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit1 = it
                                        // If a digit was entered, move focus to next field
                                        if (it.length == 1) {
                                            digit2FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit1FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            
                            // Second digit
                            OutlinedTextField(
                                value = digit2,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit2 = it
                                        // If a digit was entered, move focus to next field
                                        if (it.length == 1) {
                                            digit3FocusRequester.requestFocus()
                                        } else if (it.isEmpty()) {
                                            // If user deleted the digit, move focus back
                                            digit1FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit2FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            
                            // Third digit
                            OutlinedTextField(
                                value = digit3,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit3 = it
                                        // If a digit was entered, move focus to next field
                                        if (it.length == 1) {
                                            digit4FocusRequester.requestFocus()
                                        } else if (it.isEmpty()) {
                                            // If user deleted the digit, move focus back
                                            digit2FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit3FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            
                            // Fourth digit
                            OutlinedTextField(
                                value = digit4,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit4 = it
                                        // If a digit was entered, move focus to next field
                                        if (it.length == 1) {
                                            digit5FocusRequester.requestFocus()
                                        } else if (it.isEmpty()) {
                                            // If user deleted the digit, move focus back
                                            digit3FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit4FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            
                            // Fifth digit
                            OutlinedTextField(
                                value = digit5,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit5 = it
                                        // If a digit was entered, move focus to next field
                                        if (it.length == 1) {
                                            digit6FocusRequester.requestFocus()
                                        } else if (it.isEmpty()) {
                                            // If user deleted the digit, move focus back
                                            digit4FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit5FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            
                            // Sixth digit
                            OutlinedTextField(
                                value = digit6,
                                onValueChange = { 
                                    if (it.length <= 1 && it.all { char -> char.isDigit() }) {
                                        digit6 = it
                                        if (it.isEmpty()) {
                                            // If user deleted the digit, move focus back
                                            digit5FocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .focusRequester(digit6FocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { viewModel.submitVerificationCode(verificationCode) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = digit1.isNotEmpty() && digit2.isNotEmpty() && 
                                      digit3.isNotEmpty() && digit4.isNotEmpty() &&
                                      digit5.isNotEmpty() && digit6.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Xác nhận", fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        TextButton(
                            onClick = { 
                                digit1 = ""
                                digit2 = ""
                                digit3 = ""
                                digit4 = ""
                                digit5 = ""
                                digit6 = ""
                                viewModel.resendVerificationCode(phoneNumber, activity) 
                            }
                        ) {
                            Text("Gửi lại mã", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    is AuthState.Loading -> {
                        CircularProgressIndicator()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Đang xử lý...",
                            fontSize = 14.sp
                        )
                    }
                    
                    else -> { /* Success is handled by LaunchedEffect */ }
                }
            }
        }
    }
}
