

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:savemate/screens/login_screen.dart';
import 'package:savemate/services/auth_service.dart';

class Frontendtest extends ChangeNotifier implements AuthService {
  @override
  bool get isLoggedIn => false;

  @override
  bool get isLoading => false;

  @override
  String? get errorMessage => null;

  @override
  Future<bool> login(String email, String password) async {
    await Future.delayed(const Duration(milliseconds: 100));
    return true;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  testWidgets('Login Screen UI Test', (WidgetTester tester) async {
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<AuthService>(create: (_) => Frontendtest()),
        ],
        child: const MaterialApp(
          home: LoginScreen(),
        ),
      ),
    );

    expect(find.text('SaveMate'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.widgetWithText(ElevatedButton, 'Iniciar Sesión'), findsOneWidget);
    expect(find.text('Crear Cuenta'), findsOneWidget);

    await tester.tap(find.widgetWithText(ElevatedButton, 'Iniciar Sesión'));
    await tester.pump(); // Reconstruir el widget después del tap

    expect(find.text('Por favor ingresa tu email'), findsOneWidget);
    expect(find.text('Por favor ingresa tu contraseña'), findsOneWidget);
  });
}