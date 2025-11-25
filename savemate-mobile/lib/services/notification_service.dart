import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:logger/logger.dart';
import 'package:sms_reader/sms_reader.dart';
import 'package:flutter/material.dart';
import 'package:telephony/telephony.dart';

class NotificationService with ChangeNotifier {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  final Logger _logger = Logger();

  bool _isInitialized = false;
  bool _hasSmsPermission = false;
  bool _hasNotificationPermission = false;

  // Getters
  bool get isInitialized => _isInitialized;
  bool get hasSmsPermission => _hasSmsPermission;
  bool get hasNotificationPermission => _hasNotificationPermission;

  // Inicialización
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

  Future<void> showSavingNotification({
    required double amount,
    required String merchant,
  }) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: '¡Ahorro Automático!',
      body: 'Se han ahorrado \$${amount.toStringAsFixed(2)} de tu compra en $merchant',
      payload: 'saving',
    );
  }

  Future<void> showGoalCompletedNotification(String goalName) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: '¡Meta Completada! 🎉',
      body: 'Felicidades! Has completado tu meta: $goalName',
      payload: 'goal_completed',
    );
  }

  Future<void> showRecommendationNotification(String recommendationTitle) async {
    await showNotification(
      id: DateTime.now().millisecondsSinceEpoch.remainder(100000),
      title: 'Nueva Recomendación 💡',
      body: recommendationTitle,
      payload: 'recommendation',
    );
  }

// SMS Listener con TELEPHONY
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
          _processTransactionFromSms(transactionData);

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

  Future<void> _processTransactionFromSms(Map<String, dynamic> transactionData) async {
    try {
      // Aquí se llamaría al ApiService para procesar la transacción
      // Por ahora, solo logueamos
      _logger.i('Processing transaction from SMS: $transactionData');

      // TODO: Implementar llamada al backend
      // await _apiService.processTransactionFromNotification(
      //   userId: _currentUserId,
      //   amount: transactionData['amount'],
      //   description: transactionData['description'],
      //   merchantName: transactionData['merchant'],
      //   notificationSource: transactionData['notificationSource'],
      //   bankReference: transactionData['bankReference'],
      // );
    } catch (e) {
      _logger.e('Error processing transaction from SMS: $e');
    }
  }

  // Callback de notificación
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
  Future<void> cancelNotification(int id) async {
    try {
      await _flutterLocalNotificationsPlugin.cancel(id);
      _logger.i('Notification cancelled: $id');
    } catch (e) {
      _logger.e('Error cancelling notification: $e');
    }
  }

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


