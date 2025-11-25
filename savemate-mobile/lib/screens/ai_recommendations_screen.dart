import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../models/ai_recommendation.dart';

class AIRecommendationsScreen extends StatefulWidget {
  const AIRecommendationsScreen({Key? key}) : super(key: key);

  @override
  State<AIRecommendationsScreen> createState() => _AIRecommendationsScreenState();
}

class _AIRecommendationsScreenState extends State<AIRecommendationsScreen> {
  List<AIRecommendation> _recommendations = [];
  bool _isLoading = true;
  bool _isGenerating = false;

  @override
  void initState() {
    super.initState();
    _loadRecommendations();
  }

  Future<void> _loadRecommendations() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        final recommendations = await apiService.getActiveRecommendations(authService.user!.id!);
        _recommendations = recommendations;
      }
    } catch (e) {
      print('Error loading recommendations: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _generateRecommendations() async {
    setState(() {
      _isGenerating = true;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final apiService = Provider.of<ApiService>(context, listen: false);

      if (authService.user?.id != null) {
        await apiService.generateAllRecommendations(authService.user!.id!);
        await _loadRecommendations();
        
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Recomendaciones generadas exitosamente'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error generando recomendaciones: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() {
        _isGenerating = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Recomendaciones IA',
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        actions: [
          IconButton(
            onPressed: _isGenerating ? null : _generateRecommendations,
            icon: _isGenerating 
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadRecommendations,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _recommendations.isEmpty
                ? _buildEmptyState()
                : SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildHeader(),
                        const SizedBox(height: 24),
                        ..._recommendations.map((rec) => _buildRecommendationCard(rec)).toList(),
                        const SizedBox(height: 100),
                      ],
                    ),
                  ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(40),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                color: Theme.of(context).primaryColor.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.lightbulb_outline,
                size: 60,
                color: Theme.of(context).primaryColor,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'No hay recomendaciones',
              style: GoogleFonts.poppins(
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: Colors.grey[700],
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Genera recomendaciones personalizadas para optimizar tus ahorros',
              style: GoogleFonts.poppins(
                fontSize: 16,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ElevatedButton.icon(
              onPressed: _isGenerating ? null : _generateRecommendations,
              icon: _isGenerating 
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.auto_awesome),
              label: Text(_isGenerating ? 'Generando...' : 'Generar Recomendaciones'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    final totalPotentialSavings = _recommendations
        .where((r) => r.hasPotentialSavings)
        .fold<double>(0, (sum, r) => sum + (r.potentialSavings ?? 0));
    
    final highConfidenceCount = _recommendations
        .where((r) => r.hasHighConfidence)
        .length;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Colors.purple,
            Colors.blue,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.psychology,
                color: Colors.white,
                size: 28,
              ),
              const SizedBox(width: 12),
              Text(
                'Asistente IA',
                style: GoogleFonts.poppins(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildHeaderStat(
                  'Recomendaciones',
                  '${_recommendations.length}',
                  Colors.white,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _buildHeaderStat(
                  'Alta Confianza',
                  '$highConfidenceCount',
                  Colors.white.withOpacity(0.9),
                ),
              ),
            ],
          ),
          if (totalPotentialSavings > 0) ...[
            const SizedBox(height: 12),
            _buildHeaderStat(
              'Ahorro Potencial',
              '\$${totalPotentialSavings.toStringAsFixed(2)}',
              Colors.white,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildHeaderStat(String label, String value, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: color.withOpacity(0.9),
          ),
        ),
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  Widget _buildRecommendationCard(AIRecommendation recommendation) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
        border: Border.all(
          color: recommendation.typeColor.withOpacity(0.2),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: recommendation.typeColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  _getRecommendationIcon(recommendation.recommendationType),
                  color: recommendation.typeColor,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      recommendation.title,
                      style: GoogleFonts.poppins(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      recommendation.recommendationTypeDisplay,
                      style: GoogleFonts.poppins(
                        fontSize: 12,
                        color: recommendation.typeColor,
                      ),
                    ),
                  ],
                ),
              ),
              Row(
                children: [
                  if (recommendation.hasHighConfidence)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.green.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        recommendation.formattedConfidenceScore,
                        style: GoogleFonts.poppins(
                          fontSize: 10,
                          fontWeight: FontWeight.w500,
                          color: Colors.green,
                        ),
                      ),
                    ),
                  if (recommendation.isHighPriority) ...[
                    const SizedBox(width: 8),
                    Icon(
                      Icons.priority_high,
                      color: Colors.red,
                      size: 16,
                    ),
                  ],
                ],
              ),
            ],
          ),
          
          const SizedBox(height: 16),
          
          // Descripción
          Text(
            recommendation.description,
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.grey[700],
              height: 1.4,
            ),
          ),
          
          if (recommendation.hasPotentialSavings) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.green.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.trending_up,
                    color: Colors.green,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Ahorro potencial: ${recommendation.formattedPotentialSavings}',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Colors.green,
                    ),
                  ),
                ],
              ),
            ),
          ],
          
          const SizedBox(height: 16),
          
          // Footer
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Creada: ${recommendation.formattedCreatedDate}',
                style: GoogleFonts.poppins(
                  fontSize: 10,
                  color: Colors.grey[500],
                ),
              ),
              if (recommendation.expiresAt != null)
                Text(
                  'Expira: ${recommendation.formattedExpiresAt}',
                  style: GoogleFonts.poppins(
                    fontSize: 10,
                    color: recommendation.isExpired ? Colors.red : Colors.grey[500],
                  ),
                ),
            ],
          ),
          
          const SizedBox(height: 12),
          
          // Acciones
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: recommendation.isApplied 
                      ? null 
                      : () => _applyRecommendation(recommendation),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: recommendation.typeColor,
                    foregroundColor: Colors.white,
                  ),
                  child: Text(
                    recommendation.isApplied ? 'Aplicada' : 'Aplicar',
                    style: GoogleFonts.poppins(fontSize: 12),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              OutlinedButton(
                onPressed: () => _showRecommendationDetails(recommendation),
                style: OutlinedButton.styleFrom(
                  side: BorderSide(color: recommendation.typeColor),
                ),
                child: Text(
                  'Detalles',
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: recommendation.typeColor,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  IconData _getRecommendationIcon(RecommendationType type) {
    switch (type) {
      case RecommendationType.spendingPattern:
        return Icons.analytics;
      case RecommendationType.savingOptimization:
        return Icons.trending_up;
      case RecommendationType.goalAdjustment:
        return Icons.flag;
      case RecommendationType.roundingConfig:
        return Icons.auto_graph;
      case RecommendationType.percentageConfig:
        return Icons.percent;
      case RecommendationType.expenseReduction:
        return Icons.shopping_cart;
      case RecommendationType.incomeIncrease:
        return Icons.attach_money;
    }
  }

  Future<void> _applyRecommendation(AIRecommendation recommendation) async {
    try {
      final apiService = Provider.of<ApiService>(context, listen: false);
      await apiService.applyRecommendation(recommendation.id!);
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Recomendación aplicada exitosamente'),
          backgroundColor: Colors.green,
        ),
      );
      
      _loadRecommendations();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error aplicando recomendación: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _showRecommendationDetails(AIRecommendation recommendation) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          recommendation.title,
          style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
        ),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                recommendation.description,
                style: GoogleFonts.poppins(height: 1.4),
              ),
              const SizedBox(height: 16),
              _buildDetailRow('Tipo', recommendation.recommendationTypeDisplay),
              _buildDetailRow('Categoría', recommendation.categoryDisplay),
              _buildDetailRow('Prioridad', recommendation.priorityDisplay),
              _buildDetailRow('Confianza', recommendation.formattedConfidenceScore),
              if (recommendation.hasPotentialSavings)
                _buildDetailRow('Ahorro Potencial', recommendation.formattedPotentialSavings),
              _buildDetailRow('Estado', recommendation.statusDisplay),
              _buildDetailRow('Creada', recommendation.formattedCreatedDate),
              if (recommendation.expiresAt != null)
                _buildDetailRow('Expira', recommendation.formattedExpiresAt),
              if (recommendation.actionText != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Theme.of(context).primaryColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    'Acción sugerida: ${recommendation.actionText}',
                    style: GoogleFonts.poppins(
                      fontSize: 12,
                      fontStyle: FontStyle.italic,
                      color: Theme.of(context).primaryColor,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cerrar'),
          ),
          if (!recommendation.isApplied)
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                _applyRecommendation(recommendation);
              },
              child: const Text('Aplicar'),
            ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.grey[600],
            ),
          ),
          Text(
            value,
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}