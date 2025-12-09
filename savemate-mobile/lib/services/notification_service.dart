import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:logger/logger.dart';
// import 'package:sms_reader/sms_reader.dart'; // No necesario si usas Telephony
import 'package:flutter/material.dart';
import 'package:telephony/telephony.dart';
import 'api_service.dart';

/// Servicio singleton encargado de la gestión de notificaciones locales y la lectura de SMS.
///
/// Sus responsabilidades principales son:
/// 1. Inicializar el plugin de notificaciones locales para Android e iOS.
/// 2. Solicitar y gestionar permisos del sistema (Notificaciones y SMS).
/// 3. Escuchar mensajes SMS entrantes en segundo plano para detectar transacciones bancarias.
/// 4. Parsear el contenido de los SMS y enviarlos al backend a través de [ApiService].
/// 5. Mostrar notificaciones visuales al usuario (ahorros, metas cumplidas, etc.).
class NotificationService with ChangeNotifier {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _flutterLocalNotificationsPlugin =
  FlutterLocalNotificationsPlugin();
  final Logger _logger = Logger();

  ApiService? _apiService;

  bool _isInitialized = false;
  bool _hasSmsPermission = false;
  bool _hasNotificationPermission = false;

  // Getters
  bool get isInitialized => _isInitialized;
  bool get hasSmsPermission => _hasSmsPermission;
  bool get hasNotificationPermission => _hasNotificationPermission;

  /// Inyecta la dependencia de [ApiService] necesaria para procesar transacciones.
  ///
  /// Debe llamarse antes de que el servicio intente procesar cualquier SMS,
  /// generalmente al inicio de la aplicación o después del login.
  void setApiService(ApiService service) {
    _apiService = service;
    _logger.i('ApiService inyectado en NotificationService');
  }

  // Inicialización

  /// Punto de entrada estático para inicializar el servicio.
  ///
  /// Configura los ajustes específicos de cada plataforma (iconos para Android, permisos para iOS)
  /// y solicita los permisos iniciales.
  static Future<void> initialize(FlutterLocalNotificationsPlugin plugin) async {
    final instance = NotificationService._instance;
    instance._flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        iOS: DarwinInitializationSettings(),
      ),
      onDidReceiveNotificationResponse: instance._onNotificationTapped,
    );
    await instance._requestPermissions();
    instance._isInitialized = true;
  }

  Future<void> _initialize() async {
    try {
      // Configuración de Android
      const AndroidInitializationSettings initializationSettingsAndroid =
      AndroidInitializationSettings('@mipmap/ic_launcher');

      // Configuración de iOS
      const DarwinInitializationSettings initializationSettingsIOS =
      DarwinInitializationSettings(
        requestAlertPermission: true,
        requestBadgePermission: true,
        requestSoundPermission: true,
      );

      const InitializationSettings initializationSettings =
      InitializationSettings(
        android: initializationSettingsAndroid,
        iOS: initializationSettingsIOS,
      );

      await _flutterLocalNotificationsPlugin.initialize(
        initializationSettings,
        onDidReceiveNotificationResponse: _onNotificationTapped,
      );

      // Solicitar permisos
      await _requestPermissions();

      _isInitialized = true;
      _logger.i('NotificationService initialized successfully');
    } catch (e) {
      _logger.e('Error initializing NotificationService: $e');
    }
  }

  // Permisos

  /// Solicita los permisos necesarios al sistema operativo.
  ///
  /// Pide permiso para mostrar notificaciones y para leer SMS.
  /// Actualiza los estados [_hasNotificationPermission] y [_hasSmsPermission].
  Future<void> _requestPermissions() async {
    try {
      // Permisos de notificación
      final notificationStatus = await _flutterLocalNotificationsPlugin
          .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();

      _hasNotificationPermission = notificationStatus ?? false;

      // Permisos de SMS
      final smsStatus = await Permission.sms.request();
      _hasSmsPermission = smsStatus.isGranted;

      notifyListeners();
      _logger.i('Permissions requested - Notifications: $_hasNotificationPermission, SMS: $_hasSmsPermission');
    } catch (e) {
      _logger.e('Error requesting permissions: $e');
    }
  }

  /// Solicita explícitamente el permiso de lectura de SMS.
  ///
  /// Si el permiso es concedido, inicia inmediatamente el listener de SMS
  /// mediante [_startSmsListener].
  ///
  /// Retorna `true` si el permiso fue concedido.
  Future<bool> requestSmsPermission() async {
    try {
      final status = await Permission.sms.request();
      _hasSmsPermission = status.isGranted;
      notifyListeners();

      if (_hasSmsPermission) {
        await _startSmsListener();
      }

      return _hasSmsPermission;
    } catch (e) {
      _logger.e('Error requesting SMS permission: $e');
      return false;
    }
  }

  // Notificaciones locales

  /// Muestra una notificación local genérica.
  ///
  /// [id]: Identificador único de la notificación.
  /// [title]: Título visible.
  /// [body]: Contenido del mensaje.
  /// [payload]: Datos opcionales para manejar la acción al tocar la notificación.
  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    try {
      const AndroidNotificationDetails androidPlatformChannelSpecifics =
      AndroidNotificationDetails(
        'savemate_channel',
        'SaveMate Notifications',
        channelDescription: 'Notificaciones de SaveMate',
        importance: Importance.high,
        priority: Priority.high,
        color: Color(0xFF4CAF50),
      );

      const DarwinNotificationDetails iOSPlatformChannelSpecifics =
      DarwinNotificationDetails(
        presentAlert: true,
        presentBadge: true,
        presentSound: true,
      );

      const NotificationDetails platformChannelSpecifics = NotificationDetails(
        android: androidPlatformChannelSpecifics,
        iOS: iOSPlatformChannelSpecifics,
      );

      await _flutterLocalNotificationsPlugin.show(
        id,
        title,
        body,
        platformChannelSpecifics,
        payload: payload,
      );

      _logger.i('Notification shown: $title');
    } catch (e) {
      _logger.e('Error showing notification: $e');
    }
  }

  /// Muestra una notificación específica cuando se detecta un ahorro automático.
  Future<void> showSavingNotification({
    required double amount,
    required String merchant,
  }) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: '¡Ahorro Automático! 💰',
      body: 'Se han ahorrado \$${amount.toStringAsFixed(0)} de tu compra en $merchant',
      payload: 'saving',
    );
  }

  /// Muestra una notificación cuando el usuario completa una meta de ahorro.
  Future<void> showGoalCompletedNotification(String goalName) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: '¡Meta Completada! 🎉',
      body: 'Felicidades! Has completado tu meta: $goalName',
      payload: 'goal_completed',
    );
  }

  /// Muestra una notificación con una recomendación generada por la IA.
  Future<void> showRecommendationNotification(String recommendationTitle) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: 'Nueva Recomendación 💡',
      body: recommendationTitle,
      payload: 'recommendation',
    );
  }

// SMS Listener con TELEPHONY

  /// Inicia la escucha de SMS entrantes utilizando el paquete `Telephony`.
  ///
  /// Configura un callback que se ejecuta cada vez que llega un mensaje.
  /// Si el cuerpo del mensaje no es nulo, lo envía a [_processSms].
  Future<void> _startSmsListener() async {
    try {
      if (!_hasSmsPermission) {
        _logger.w("❌ Permiso de SMS no concedido");
        return;
      }

      final Telephony telephony = Telephony.instance;

      telephony.listenIncomingSms(
        onNewMessage: (SmsMessage sms) {
          if (sms.body != null) {
            _processSms(sms.body! as SmsMessage);
          }
        },
        listenInBackground: false,
      );

      _logger.i("✅ Listener de SMS iniciado correctamente");
    } catch (e) {
      _logger.e("🔥 Error iniciando SMS listener: $e");
    }
  }


  /// Analiza si un SMS entrante es relevante para la aplicación.
  ///
  /// Filtra el mensaje buscando palabras clave bancarias (ej. "compra", "Bancolombia").
  /// Si es relevante, extrae los datos con [_parseBankingSms] e inicia el proceso
  /// de transacción con el backend y muestra una notificación local de detección.
  void _processSms(SmsMessage sms) {
    try {
      final message = sms.body?.toLowerCase() ?? '';

      // Palabras clave para detectar transacciones bancarias
      final bankingKeywords = [
        'compra', 'pago', 'débito', 'consumo',
        'bancolombia', 'daviplata', 'nequi',
        '\$', 'pesos', 'cop'
      ];

      final isBankingMessage = bankingKeywords.any((keyword) => message.contains(keyword));

      if (isBankingMessage) {
        _logger.i('Banking SMS detected: ${sms.body}');

        final transactionData = _parseBankingSms(sms.body ?? '');

        if (transactionData != null) {
          // Procesar con el backend
          _processTransactionFromSms(transactionData);

          // Mostrar notificación inmediata de detección
          showNotification(
            id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
            title: 'Transacción Detectada',
            body: 'Se ha detectado una transacción: ${transactionData['description']}',
            payload: 'transaction_detected',
          );
        }
      }
    } catch (e) {
      _logger.e('Error processing SMS: $e');
    }
  }

  /// Extrae información estructurada (monto, comercio, tipo) del cuerpo de un SMS.
  ///
  /// Utiliza Expresiones Regulares (RegExp) para identificar patrones de dinero y texto.
  /// Retorna un [Map] con los datos o `null` si no logra parsear el mensaje.
  Map<String, dynamic>? _parseBankingSms(String smsBody) {
    try {
      // Implementar lógica de parseo de SMS
      // Esto es una versión simplificada

      // Extraer monto
      final amountRegex = RegExp(r'\$?([\d,]+\.?\d*)\s*(?:pesos|cop)?');
      final amountMatch = amountRegex.firstMatch(smsBody);

      if (amountMatch == null) return null;

      final amount = double.parse(amountMatch.group(1)!.replaceAll(',', ''));

      // Extraer comerciante
      final merchantRegex = RegExp(r'en\s+([^.]+)|a\s+([^.]+)');
      final merchantMatch = merchantRegex.firstMatch(smsBody);

      String merchant = 'Comercio no identificado';
      if (merchantMatch != null) {
        merchant = merchantMatch.group(1) ?? merchantMatch.group(2) ?? merchant;
      }

      // Determinar tipo de transacción
      final isExpense = smsBody.toLowerCase().contains(RegExp(r'compra|pago|débito|consumo'));
      final isIncome = smsBody.toLowerCase().contains(RegExp(r'abono|crédito|depósito|recibiste'));

      String transactionType = 'EXPENSE';
      if (isIncome) transactionType = 'INCOME';

      return {
        'amount': amount,
        'merchant': merchant,
        'description': 'Transacción en $merchant',
        'transactionType': transactionType,
        'notificationSource': 'SMS',
        'bankReference': DateTime.now().millisecondsSinceEpoch.toString(),
      };
    } catch (e) {
      _logger.e('Error parsing banking SMS: $e');
      return null;
    }
  }

  /// Envía los datos de una transacción detectada por SMS al servidor.
  ///
  /// Realiza una llamada HTTP POST a través de [ApiService.processTransactionFromNotification].
  ///
  /// Si el backend responde que se generó un ahorro ([transaction.savingAmount] > 0),
  /// dispara una notificación de ahorro al usuario.
  Future<void> _processTransactionFromSms(Map<String, dynamic> transactionData) async {
    try {
      _logger.i('Procesando transacción desde SMS: $transactionData');

      // 1. Verificar si el ApiService está disponible
      if (_apiService == null) {
        _logger.w('⚠️ ApiService no inicializado en NotificationService. No se puede enviar al backend.');
        return;
      }

      // 2. Verificar si hay un usuario autenticado
      final currentUser = _apiService!.currentUser;
      if (currentUser == null || currentUser.id == null) {
        _logger.w('⚠️ No hay usuario logueado. Transacción ignorada.');
        return;
      }

      // 3. Llamada al Backend
      final transaction = await _apiService!.processTransactionFromNotification(
        userId: currentUser.id!, // ID del usuario actual
        amount: transactionData['amount'],
        description: transactionData['description'],
        merchantName: transactionData['merchant'],
        notificationSource: 'SMS',
        bankReference: transactionData['bankReference'],
      );

      _logger.i('✅ Transacción enviada al backend exitosamente. ID: ${transaction.id}');

      // 4. Si el backend calculó un ahorro, notificar al usuario
      if (transaction.savingAmount != null && transaction.savingAmount! > 0) {
        showSavingNotification(
          amount: transaction.savingAmount!,
          merchant: transaction.merchantName ?? 'Comercio',
        );
      }

    } catch (e) {
      _logger.e('🔥 Error enviando transacción al backend: $e');
    }
  }

  // Callback de notificación

  /// Maneja la acción del usuario al tocar una notificación.
  ///
  /// Enruta la navegación de la app basándose en el [response.payload].
  void _onNotificationTapped(NotificationResponse response) {
    _logger.i('Notification tapped: ${response.payload}');

    // Manejar diferentes tipos de notificaciones
    switch (response.payload) {
      case 'saving':
      // Navegar a pantalla de ahorros
        break;
      case 'goal_completed':
      // Navegar a pantalla de metas
        break;
      case 'recommendation':
      // Navegar a pantalla de recomendaciones
        break;
      case 'transaction_detected':
      // Navegar a pantalla de transacciones
        break;
    }
  }

  // Cancelar notificaciones

  /// Cancela una notificación específica por su [id].
  Future<void> cancelNotification(int id) async {
    try {
      await _flutterLocalNotificationsPlugin.cancel(id);
      _logger.i('Notification cancelled: $id');
    } catch (e) {
      _logger.e('Error cancelling notification: $e');
    }
  }

  /// Cancela todas las notificaciones pendientes o visibles.
  Future<void> cancelAllNotifications() async {
    try {
      await _flutterLocalNotificationsPlugin.cancelAll();
      _logger.i('All notifications cancelled');
    } catch (e) {
      _logger.e('Error cancelling all notifications: $e');
    }
  }

  // Obtener notificaciones pendientes
  Future<List<PendingNotificationRequest>> getPendingNotifications() async {
    try {
      return await _flutterLocalNotificationsPlugin.pendingNotificationRequests();
    } catch (e) {
      _logger.e('Error getting pending notifications: $e');
      return [];
    }
  }
}