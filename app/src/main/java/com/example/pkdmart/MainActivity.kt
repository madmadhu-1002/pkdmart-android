package com.example.pkdmart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pkdmart.ui.theme.PkdmartTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PkdmartTheme {
                PkdmartNativeApp()
            }
        }
    }
}

private const val PREFS_NAME = "pkdmart_prefs"
private const val CART_JSON_KEY = "cart_json"
private const val CHECKOUT_UPI_ID = "paytmqr18f80ytzoi@paytm"
private const val CHECKOUT_MERCHANT_CATEGORY = "5411"

// PKDMart API checkout defaults
private const val API_USER_ID = "68eba42b109343d1f25edc3c"
private const val API_USER_NAME = "Mahi"
private const val API_USER_MOBILE = "9000000000"
private const val GOOGLE_WEB_CLIENT_ID = "169534830935-mdubla6cq9q99r3jm424tudsepo41p9h.apps.googleusercontent.com"
private const val AUTH_PREFS_NAME = "pkdmart_auth"
private const val AUTH_USER_ID_KEY = "auth_user_id"
private const val AUTH_USER_EMAIL_KEY = "auth_user_email"
private const val AUTH_USER_NAME_KEY = "auth_user_name"

// -------- API --------

data class CategoryDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val imageUrl: String? = null
)

data class ProductDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val category: String? = null,
    val sellingPrice: Double? = null,
    val mrp: Double? = null,
    val imageUrl: String? = null,
    val unit: String? = null,
    val description: String? = null
)

data class CategoriesResponse(
    val success: Boolean,
    val categories: List<CategoryDto> = emptyList()
)

data class ProductsResponse(
    val success: Boolean,
    val products: List<ProductDto> = emptyList()
)

data class ProductDetailResponse(
    val success: Boolean,
    val product: ProductDto
)

data class OrderModeResponse(
    val isQuickActive: Boolean = false,
    val isScheduledActive: Boolean = true
)

data class AddressLocationPayload(
    val label: String,
    val formattedAddress: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean = true
)

data class CreateOrderAddressPayload(
    val name: String,
    val mobile: String,
    val location: AddressLocationPayload
)

data class CreateOrderItemPayload(
    val productId: String,
    val quantity: Int
)

data class CreateOrderRequest(
    val userId: String,
    val address: CreateOrderAddressPayload,
    val paymentMethod: String = "cod",
    val orderType: String,
    val deliverySlot: String? = null,
    val products: List<CreateOrderItemPayload>
)

data class CreatedOrderPayload(
    @SerializedName("_id") val id: String? = null,
    val redirectUrl: String? = null
)

data class CreateOrderResponse(
    val success: Boolean = false,
    val order: CreatedOrderPayload? = null,
    val error: String? = null,
    val otp: String? = null
)

data class GoogleMobileAuthRequest(
    val idToken: String
)

data class GoogleMobileAuthUser(
    @SerializedName("_id") val id: String,
    val email: String,
    val name: String? = null
)

data class GoogleMobileAuthResponse(
    val success: Boolean = false,
    val user: GoogleMobileAuthUser? = null,
    val error: String? = null
)

interface PkdmartApi {
    @GET("api/categories")
    suspend fun getCategories(): CategoriesResponse

    @GET("api/products")
    suspend fun getProducts(): ProductsResponse

    @GET("api/products/{id}")
    suspend fun getProductDetail(@Path("id") id: String): ProductDetailResponse

    @GET("api/order-mode")
    suspend fun getOrderMode(): OrderModeResponse

    @POST("api/order/create-order")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    @POST("api/auth/google/mobile")
    suspend fun googleMobileAuth(@Body request: GoogleMobileAuthRequest): GoogleMobileAuthResponse
}

private object ApiClient {
    val api: PkdmartApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://pkdmart.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PkdmartApi::class.java)
    }
}

data class UiCategory(
    val name: String,
    val color: Color,
    val imageUrl: String? = null
)

data class UiProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: String,
    val priceValue: Int,
    val unit: String,
    val imageUrl: String?
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()

    var categories by mutableStateOf(listOf(UiCategory("All", Color(0xFFE8F5E9), null)))
    var products by mutableStateOf(emptyList<UiProduct>())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    var selectedProductDetail by mutableStateOf<ProductDto?>(null)
    var detailLoading by mutableStateOf(false)
    var detailError by mutableStateOf<String?>(null)

    val cartQuantities = mutableStateMapOf<String, Int>()

    init {
        loadCartFromDisk()
    }

    private fun loadCartFromDisk() {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString(CART_JSON_KEY, null)
                if (!json.isNullOrBlank()) {
                    val type = object : TypeToken<Map<String, Int>>() {}.type
                    val map: Map<String, Int> = gson.fromJson(json, type) ?: emptyMap()
                    cartQuantities.clear()
                    map.forEach { (k, v) -> if (v > 0) cartQuantities[k] = v.coerceAtMost(99) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveCartToDisk() {
        viewModelScope.launch {
            try {
                val cleanMap = cartQuantities.filterValues { it > 0 }
                val json = gson.toJson(cleanMap)
                val prefs = getApplication<Application>().applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(CART_JSON_KEY, json).apply()
            } catch (_: Exception) {
            }
        }
    }

    fun load() {
        if (!loading && products.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val catResp = ApiClient.api.getCategories()
                val prodResp = ApiClient.api.getProducts()

                val apiCats = catResp.categories.mapIndexed { index, c ->
                    UiCategory(c.name, chipPalette[index % chipPalette.size], c.imageUrl)
                }
                categories = listOf(UiCategory("All", Color(0xFFE8F5E9), null)) + apiCats

                products = prodResp.products.map { p ->
                    val priceValue = (p.sellingPrice ?: p.mrp ?: 0.0).toInt().coerceAtLeast(0)
                    UiProduct(
                        id = p.id,
                        name = p.name,
                        category = p.category ?: "Others",
                        price = "₹$priceValue",
                        priceValue = priceValue,
                        unit = p.unit ?: "1 pc",
                        imageUrl = p.imageUrl
                    )
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load data"
            } finally {
                loading = false
            }
        }
    }

    fun addToCart(productId: String) {
        val current = cartQuantities[productId] ?: 0
        cartQuantities[productId] = (current + 1).coerceAtMost(99)
        saveCartToDisk()
    }

    fun decrementFromCart(productId: String) {
        val current = cartQuantities[productId] ?: return
        if (current <= 1) cartQuantities.remove(productId) else cartQuantities[productId] = current - 1
        saveCartToDisk()
    }

    fun removeFromCart(productId: String) {
        cartQuantities.remove(productId)
        saveCartToDisk()
    }

    fun clearCart() {
        cartQuantities.clear()
        saveCartToDisk()
    }

    fun cartCount(): Int = cartQuantities.values.sum().coerceAtLeast(0)

    fun loadProductDetail(id: String) {
        viewModelScope.launch {
            detailLoading = true
            detailError = null
            selectedProductDetail = null
            try {
                selectedProductDetail = ApiClient.api.getProductDetail(id).product
            } catch (e: Exception) {
                detailError = e.message ?: "Failed to load product detail"
            } finally {
                detailLoading = false
            }
        }
    }

    fun clearProductDetail() {
        selectedProductDetail = null
        detailError = null
        detailLoading = false
    }
}

private val chipPalette = listOf(
    Color(0xFFE3F2FD),
    Color(0xFFFFF3E0),
    Color(0xFFE8F5E9),
    Color(0xFFF3E5F5),
    Color(0xFFFFEBEE),
    Color(0xFFE0F2F1)
)

private fun requiredRuntimePermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return permissions.toTypedArray()
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    fusedClient: FusedLocationProviderClient,
    callback: LocationCallback,
    onUpdate: (String) -> Unit
) {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L
    )
        .setMinUpdateDistanceMeters(5f)
        .build()
    
    fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
}

@SuppressLint("MissingPermission")
private fun readLastKnownCoordinates(context: Context): String? {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return null

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            // Callback will handle display
        }
    }
    
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    val best: Location = providers.mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time } ?: return null

    return "Lat ${"%.6f".format(best.latitude)}, Lng ${"%.6f".format(best.longitude)}"
}

private fun googleStatusHint(code: Int): String = when (code) {
    7 -> "NETWORK_ERROR"
    8 -> "INTERNAL_ERROR"
    10 -> "DEVELOPER_ERROR (check SHA-1/SHA-256 + OAuth client)"
    13 -> "ERROR"
    16 -> "CANCELED"
    12500 -> "SIGN_IN_FAILED"
    12501 -> "SIGN_IN_CANCELLED"
    12502 -> "SIGN_IN_CURRENTLY_IN_PROGRESS"
    else -> "UNKNOWN"
}

private enum class PendingAction {
    NONE,
    OPEN_CART,
    OPEN_ORDERS,
    ADD_TO_CART
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PkdmartNativeApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(context.applicationContext as Application) as T
            }
        }
    )
    var selectedTab by remember { mutableStateOf(0) }
    var productDetailOpen by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("Location: requesting permission…") }
    var lastBackPressMs by remember { mutableStateOf(0L) }
    val appScope = rememberCoroutineScope()

    val authPrefs = remember {
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var authUserId by remember { mutableStateOf(authPrefs.getString(AUTH_USER_ID_KEY, null)) }
    var authUserEmail by remember { mutableStateOf(authPrefs.getString(AUTH_USER_EMAIL_KEY, null)) }
    var authUserName by remember { mutableStateOf(authPrefs.getString(AUTH_USER_NAME_KEY, null)) }
    val isLoggedIn = !authUserId.isNullOrBlank()
    var showLoginDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf(PendingAction.NONE) }
    var pendingAddProductId by remember { mutableStateOf<String?>(null) }
    var authInProgress by remember { mutableStateOf(false) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    fun runPendingActionAfterLogin() {
        when (pendingAction) {
            PendingAction.OPEN_CART -> selectedTab = 2
            PendingAction.OPEN_ORDERS -> selectedTab = 3
            PendingAction.ADD_TO_CART -> pendingAddProductId?.let { vm.addToCart(it) }
            PendingAction.NONE -> {}
        }
        pendingAction = PendingAction.NONE
        pendingAddProductId = null
    }

    fun persistAuth(userId: String, email: String?, name: String?) {
        authPrefs.edit()
            .putString(AUTH_USER_ID_KEY, userId)
            .putString(AUTH_USER_EMAIL_KEY, email)
            .putString(AUTH_USER_NAME_KEY, name)
            .apply()
        authUserId = userId
        authUserEmail = email
        authUserName = name
    }

    fun authenticateWithBackend(idToken: String, onSuccess: () -> Unit = {}) {
        authInProgress = true
        appScope.launch {
            try {
                val authRes = ApiClient.api.googleMobileAuth(GoogleMobileAuthRequest(idToken = idToken))
                val userId = authRes.user?.id
                if (authRes.success && !userId.isNullOrBlank()) {
                    persistAuth(userId, authRes.user?.email, authRes.user?.name)
                    onSuccess()
                } else {
                    Toast.makeText(context, authRes.error ?: "Login failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Login failed", Toast.LENGTH_LONG).show()
            } finally {
                authInProgress = false
            }
        }
    }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    locationText = "📍 Lat ${"%.6f".format(location.latitude)}, Lng ${"%.6f".format(location.longitude)}"
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            startLocationUpdates(fusedClient, locationCallback) { coords ->
                locationText = coords
            }
        } else {
            locationText = "Location permission denied"
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            val reason = if (result.resultCode == Activity.RESULT_CANCELED) "cancelled by user" else "resultCode=${result.resultCode}"
            Toast.makeText(context, "Google sign-in not completed ($reason)", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(context, "Google token missing", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            authenticateWithBackend(idToken) {
                Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
                runPendingActionAfterLogin()
            }
        } catch (e: ApiException) {
            Toast.makeText(
                context,
                "Google error: ${e.statusCode} (${googleStatusHint(e.statusCode)})",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Google sign-in failed", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.load()

        // Auto Google Sign-In like tutorial flow (if Google account already authorized)
        if (authUserId.isNullOrBlank()) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            val cachedToken = lastAccount?.idToken
            if (!cachedToken.isNullOrBlank()) {
                authenticateWithBackend(cachedToken)
            } else {
                googleSignInClient.silentSignIn()
                    .addOnSuccessListener { acct ->
                        val token = acct.idToken
                        if (!token.isNullOrBlank() && authUserId.isNullOrBlank()) {
                            authenticateWithBackend(token)
                        }
                    }
            }
        }

        val permissions = requiredRuntimePermissions()
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startLocationUpdates(fusedClient, locationCallback) { coords ->
                locationText = coords
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    fun navigateToTab(tab: Int) {
        selectedTab = tab
        if (productDetailOpen) {
            productDetailOpen = false
            vm.clearProductDetail()
        }
    }

    fun requestLoginFor(action: PendingAction, productId: String? = null) {
        if (isLoggedIn) {
            when (action) {
                PendingAction.OPEN_CART -> navigateToTab(2)
                PendingAction.OPEN_ORDERS -> navigateToTab(3)
                PendingAction.ADD_TO_CART -> productId?.let { vm.addToCart(it) }
                PendingAction.NONE -> {}
            }
            return
        }
        pendingAction = action
        pendingAddProductId = productId
        showLoginDialog = true
    }

    BackHandler {
        when {
            productDetailOpen -> {
                productDetailOpen = false
                vm.clearProductDetail()
            }
            selectedTab != 0 -> {
                selectedTab = 0
            }
            else -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPressMs < 2000) {
                    (context as? ComponentActivity)?.moveTaskToBack(true)
                } else {
                    lastBackPressMs = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.pkdmart_logo),
                            contentDescription = "PKD Mart Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text("PKD Mart", fontWeight = FontWeight.Bold)
                            Text("Delivery in 15 mins", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            if (isLoggedIn) {
                                val nameOrEmail = authUserName?.takeIf { it.isNotBlank() } ?: authUserEmail
                                if (!nameOrEmail.isNullOrBlank()) {
                                    Text(
                                        "Hi, $nameOrEmail",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(locationText, style = MaterialTheme.typography.labelSmall, color = Color(0xFF455A64), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { navigateToTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { navigateToTab(1) }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Categories") })
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { requestLoginFor(PendingAction.OPEN_CART) },
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    label = { Text(if (vm.cartCount() > 0) "Cart (${vm.cartCount()})" else "Cart") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { requestLoginFor(PendingAction.OPEN_ORDERS) },
                    icon = { Icon(Icons.Default.Menu, null) },
                    label = { Text("Orders") }
                )
            }
        }
    ) { innerPadding ->
        if (productDetailOpen) {
            ProductDetailScreen(
                vm = vm,
                modifier = Modifier.padding(innerPadding),
                onBack = {
                    productDetailOpen = false
                    vm.clearProductDetail()
                },
                isLoggedIn = isLoggedIn,
                onRequireLoginAddToCart = { productId ->
                    requestLoginFor(PendingAction.ADD_TO_CART, productId)
                }
            )
        } else {
            when (selectedTab) {
                0 -> HomeScreen(
                    vm,
                    Modifier.padding(innerPadding),
                    onProductClick = { id ->
                        productDetailOpen = true
                        vm.loadProductDetail(id)
                    },
                    onRequireLoginAddToCart = { productId ->
                        requestLoginFor(PendingAction.ADD_TO_CART, productId)
                    },
                    isLoggedIn = isLoggedIn
                )
                1 -> CategoriesScreen(vm, Modifier.padding(innerPadding))
                2 -> CartScreen(vm, Modifier.padding(innerPadding), currentUserId = authUserId)
                else -> SimpleTab("No orders yet", Modifier.padding(innerPadding))
            }
        }
    }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showLoginDialog = false
                pendingAction = PendingAction.NONE
                pendingAddProductId = null
            },
            title = { Text("Login required") },
            text = { Text("Please sign in with Google to continue") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                ) { Text(if (authInProgress) "Signing in..." else "Sign in") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                        pendingAction = PendingAction.NONE
                        pendingAddProductId = null
                    }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
    onProductClick: (String) -> Unit = {},
    onRequireLoginAddToCart: (String) -> Unit = {},
    isLoggedIn: Boolean = true
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = vm.products.filter {
        val queryMatch = search.isBlank() || it.name.contains(search, true)
        val catMatch = selectedCategory == "All" || it.category.equals(selectedCategory, true)
        queryMatch && catMatch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Free Delivery", fontWeight = FontWeight.Bold)
                        Text("On orders above ₹199", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("🎉", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Text("Shop by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.categories) { cat ->
                    val selected = selectedCategory == cat.name
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else cat.color,
                        onClick = { selectedCategory = cat.name }
                    ) {
                        Text(
                            cat.name,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item { Text("Popular Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

        if (vm.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        vm.error?.let { msg ->
            item {
                Text("Failed to load: $msg", color = Color.Red)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.load() }) { Text("Retry") }
            }
        }

        items(filtered.take(50), key = { it.id }) { product ->
            ProductRowCard(
                product,
                onClick = { onProductClick(product.id) },
                onAdd = {
                    if (isLoggedIn) vm.addToCart(product.id)
                    else onRequireLoginAddToCart(product.id)
                },
                onDecrement = { vm.decrementFromCart(product.id) },
                cartQty = vm.cartQuantities[product.id] ?: 0
            )
        }
    }
}

@Composable
private fun ProductRowCard(
    product: UiProduct,
    onClick: () -> Unit = {},
    onAdd: () -> Unit = {},
    onDecrement: () -> Unit = {},
    cartQty: Int = 0
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFEDEDED)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFEDEDED)),
                    contentAlignment = Alignment.Center
                ) { Text(product.name.first().toString(), fontWeight = FontWeight.Bold) }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(product.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(product.price, fontWeight = FontWeight.Bold)
            }

            if (cartQty > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onDecrement,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("-") }
                    Text(cartQty.toString(), fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("+") }
                }
            } else {
                Button(onClick = onAdd, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("ADD")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    isLoggedIn: Boolean = true,
    onRequireLoginAddToCart: (String) -> Unit = {}
) {
    val detail = vm.selectedProductDetail

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Button(onClick = onBack, shape = RoundedCornerShape(10.dp)) { Text("← Back") }
        }

        if (vm.detailLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        vm.detailError?.let { msg ->
            item { Text("Failed to load: $msg", color = Color.Red) }
        }

        detail?.let { p ->
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!p.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = p.imageUrl,
                                contentDescription = p.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Price: ₹${(p.sellingPrice ?: p.mrp ?: 0.0).toInt()}", fontWeight = FontWeight.SemiBold)
                        Text("Unit: ${p.unit ?: "1 pc"}")
                        Text("Category: ${p.category ?: "Others"}")
                        Text(p.description ?: "No description available.")
                        val currentQty = vm.cartQuantities[p.id] ?: 0
                        if (currentQty > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { vm.decrementFromCart(p.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("-") }
                                Text(currentQty.toString(), fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        if (isLoggedIn) vm.addToCart(p.id)
                                        else onRequireLoginAddToCart(p.id)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) { Text("+") }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (isLoggedIn) vm.addToCart(p.id)
                                    else onRequireLoginAddToCart(p.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("ADD TO CART")
                            }
                        }
                    }
                }
            }

            val related = vm.products.filter {
                it.id != p.id && it.category.equals(p.category ?: "", ignoreCase = true)
            }.take(10)

            if (related.isNotEmpty()) {
                item {
                    Text("Related Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(related, key = { it.id }) { rel ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .size(width = 170.dp, height = 220.dp)
                                    .clickable { vm.loadProductDetail(rel.id) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AsyncImage(
                                        model = rel.imageUrl,
                                        contentDescription = rel.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEDEDED)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(rel.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(rel.unit, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(rel.price, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(vm: HomeViewModel, modifier: Modifier = Modifier) {
    val categoryList = vm.categories.filter { it.name != "All" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text("All Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Browse by section", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        items(categoryList) { cat ->
            val count = vm.products.count { it.category.equals(cat.name, ignoreCase = true) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!cat.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = cat.imageUrl,
                                contentDescription = cat.name,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(cat.color),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(cat.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.name.first().toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(cat.name, fontWeight = FontWeight.SemiBold)
                            Text("$count products", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                }
            }
        }
    }
}

private fun launchUpiIntent(context: Context, amount: Int) {
    val txId = "PKD${System.currentTimeMillis()}"
    val uri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", CHECKOUT_UPI_ID)
        .appendQueryParameter("pn", "Kosigi Mahidhar")
        .appendQueryParameter("tn", "PKDMart Order")
        .appendQueryParameter("mc", CHECKOUT_MERCHANT_CATEGORY)
        .appendQueryParameter("tr", txId)
        .appendQueryParameter("tid", txId)
        .appendQueryParameter("am", amount.toString())
        .appendQueryParameter("cu", "INR")
        .build()

    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(Intent.createChooser(intent, "Pay with UPI app"))
    } catch (_: Exception) {
        Toast.makeText(context, "No UPI app found on this device", Toast.LENGTH_SHORT).show()
    }
}

private val defaultApiAddress = AddressLocationPayload(
    label = "Home",
    formattedAddress = "APSRTC Bus Station, Pattikonda - Guntakal Rd, Pattikonda, Andhra Pradesh 518380, India",
    street = "Pattikonda - Guntakal Road",
    city = "Pattikonda",
    state = "Andhra Pradesh",
    zipCode = "518380",
    country = "India",
    lat = 15.3943402,
    lng = 77.4983354,
    isDefault = true
)

private suspend fun createServerOrder(
    userId: String,
    itemsInCart: List<Pair<UiProduct, Int>>,
    paymentMethod: String
): CreateOrderResponse {
    val mode = runCatching { ApiClient.api.getOrderMode() }.getOrNull()
    val useQuick = mode?.isQuickActive == true
    val orderType = if (useQuick) "quick" else "scheduled"
    val slot = if (orderType == "scheduled") "6-8 am" else null

    val payload = CreateOrderRequest(
        userId = userId,
        address = CreateOrderAddressPayload(
            name = API_USER_NAME,
            mobile = API_USER_MOBILE,
            location = defaultApiAddress
        ),
        paymentMethod = paymentMethod,
        orderType = orderType,
        deliverySlot = slot,
        products = itemsInCart.map { (product, qty) ->
            CreateOrderItemPayload(productId = product.id, quantity = qty)
        }
    )

    return ApiClient.api.createOrder(payload)
}

@Composable
private fun CartScreen(
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
    currentUserId: String? = null
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var placingOrder by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val itemsInCart = vm.cartQuantities.mapNotNull { (id, qty) ->
        val product = vm.products.firstOrNull { it.id == id } ?: return@mapNotNull null
        product to qty.coerceAtLeast(1)
    }
    val effectiveUserId = currentUserId ?: API_USER_ID

    fun placeOrder(paymentMethod: String) {
        scope.launch {
            placingOrder = true
            try {
                val response = createServerOrder(effectiveUserId, itemsInCart, paymentMethod)
                if (response.success && response.order != null) {
                    vm.clearCart()
                    val redirectUrl = response.order.redirectUrl
                    if (!redirectUrl.isNullOrBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)))
                    }
                    Toast.makeText(
                        context,
                        "Order placed${response.order.id?.let { " (#$it)" } ?: ""}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        response.error ?: "Order creation failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: "Unable to place order",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                placingOrder = false
            }
        }
    }

    val unavailableCount = vm.cartQuantities.size - itemsInCart.size
    val subtotal = itemsInCart.sumOf { (p, qty) -> p.priceValue * qty }
    val delivery = if (itemsInCart.isEmpty() || subtotal >= 199) 0 else 25
    val grandTotal = subtotal + delivery

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text("Your Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Review items before checkout", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        if (unavailableCount > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$unavailableCount unavailable item(s) were hidden", style = MaterialTheme.typography.bodySmall)
                        Text("Refresh", color = Color(0xFFEF6C00), modifier = Modifier.clickable { vm.load() })
                    }
                }
            }
        }

        if (itemsInCart.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛒", style = MaterialTheme.typography.headlineLarge)
                        Text("Your cart is empty", fontWeight = FontWeight.SemiBold)
                        Text("Add products from Home screen", color = Color.Gray)
                    }
                }
            }
        } else {
            items(itemsInCart, key = { it.first.id }) { (product, qty) ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            vm.removeFromCart(product.id)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text("Delete", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFEDEDED)),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text("₹${product.priceValue} x $qty = ₹${product.priceValue * qty}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { vm.decrementFromCart(product.id) }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("-") }
                                Text(qty.toString(), fontWeight = FontWeight.Bold)
                                Button(onClick = { vm.addToCart(product.id) }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+") }
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal")
                            Text("₹$subtotal")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery")
                            Text(if (delivery == 0) "FREE" else "₹$delivery")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text("₹$grandTotal", fontWeight = FontWeight.Bold)
                        }
                        if (delivery > 0) {
                            Text("Add ₹${199 - subtotal} more for free delivery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.clearCart() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5))
                    ) { Text("Clear Cart") }
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = grandTotal > 0 && !placingOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text(if (placingOrder) "Placing..." else "Place Order") }
                }
            }
        }
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { if (!placingOrder) showPaymentDialog = false },
            title = { Text("Choose Payment Method") },
            text = { Text("Select COD or PhonePe to continue") },
            confirmButton = {
                TextButton(
                    enabled = !placingOrder,
                    onClick = {
                        showPaymentDialog = false
                        placeOrder("phonepe")
                    }
                ) { Text("PhonePe") }
            },
            dismissButton = {
                TextButton(
                    enabled = !placingOrder,
                    onClick = {
                        showPaymentDialog = false
                        placeOrder("cod")
                    }
                ) { Text("COD") }
            }
        )
    }
}

@Composable
private fun SimpleTab(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

