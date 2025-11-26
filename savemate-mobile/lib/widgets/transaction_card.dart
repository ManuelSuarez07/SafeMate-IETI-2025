import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../models/transaction.dart';

class TransactionCard extends StatelessWidget {
  final Transaction transaction;
  final VoidCallback? onTap;
  final bool showDetails;

  const TransactionCard({
    Key? key,
    required this.transaction,
    this.onTap,
    this.showDetails = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: transaction.transactionTypeColor.withOpacity(0.2),
              width: 1,
            ),
          ),
          child: Column(
            children: [
              Row(
                children: [
                  Container(
                    width: 48, height: 48,
                    decoration: BoxDecoration(
                      color: transaction.transactionTypeColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      _getTransactionIcon(transaction.transactionType),
                      color: transaction.transactionTypeColor,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 16),

                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          transaction.description,
                          style: GoogleFonts.poppins(fontSize: 16, fontWeight: FontWeight.w600),
                          maxLines: 2, overflow: TextOverflow.ellipsis,
                        ),
                        if (transaction.merchantName != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            transaction.merchantName!,
                            style: GoogleFonts.poppins(fontSize: 14, color: Colors.grey[600]),
                            maxLines: 1, overflow: TextOverflow.ellipsis,
                          ),
                        ],
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Text(
                              transaction.formattedShortDate,
                              style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[500]),
                            ),
                            const SizedBox(width: 12),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: transaction.statusColor.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                transaction.statusDisplay,
                                style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w500, color: transaction.statusColor),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),

                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        transaction.formattedAmount,
                        style: GoogleFonts.poppins(fontSize: 16, fontWeight: FontWeight.bold, color: transaction.transactionTypeColor),
                      ),
                      if (transaction.hasSaving) ...[
                        const SizedBox(height: 4),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(color: Colors.green.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                          child: Row(mainAxisSize: MainAxisSize.min, children: [
                            Icon(Icons.savings, size: 12, color: Colors.green),
                            const SizedBox(width: 4),
                            Text('+${transaction.formattedSavingAmount}', style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w600, color: Colors.green)),
                          ]),
                        ),
                      ],
                    ],
                  ),
                ],
              ),

              if (showDetails && _hasAdditionalDetails(transaction)) ...[
                const SizedBox(height: 12),
                const Divider(height: 1),
                const SizedBox(height: 12),
                _buildAdditionalDetails(context, transaction),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAdditionalDetails(BuildContext context, Transaction transaction) {
    return Column(
      children: [
        if (transaction.originalAmount != null && transaction.roundedAmount != null) ...[
          _buildDetailRow(context, 'Redondeo automático', '\$${transaction.originalAmount!.toStringAsFixed(2)} → \$${transaction.roundedAmount!.toStringAsFixed(2)}', Icons.auto_graph, Theme.of(context).primaryColor),
        ],
        if (transaction.notificationSource != null) ...[
          const SizedBox(height: 8),
          _buildDetailRow(context, 'Fuente', transaction.notificationSource!, Icons.source, Colors.blue),
        ],
        if (transaction.bankReference != null) ...[
          const SizedBox(height: 8),
          _buildDetailRow(context, 'Referencia', transaction.bankReference!, Icons.receipt, Colors.grey),
        ],
      ],
    );
  }

  Widget _buildDetailRow(BuildContext context, String label, String value, IconData icon, Color color) {
    return Row(children: [
      Icon(icon, size: 16, color: color),
      const SizedBox(width: 8),
      Text('$label: ', style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[600])),
      Expanded(child: Text(value, style: GoogleFonts.poppins(fontSize: 12, fontWeight: FontWeight.w500, color: color))),
    ]);
  }

  bool _hasAdditionalDetails(Transaction transaction) {
    return transaction.originalAmount != null || transaction.notificationSource != null || transaction.bankReference != null;
  }

  // ✅ [CORRECCIÓN] Se agregó el caso .withdrawal
  IconData _getTransactionIcon(TransactionType type) {
    switch (type) {
      case TransactionType.expense: return Icons.shopping_cart;
      case TransactionType.income: return Icons.attach_money;
      case TransactionType.saving: return Icons.savings;
      case TransactionType.fee: return Icons.receipt;
      case TransactionType.withdrawal: return Icons.arrow_circle_up;
    }
  }
}

class CompactTransactionCard extends StatelessWidget {
  final Transaction transaction;
  final VoidCallback? onTap;

  const CompactTransactionCard({Key? key, required this.transaction, this.onTap}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.grey[200]!, width: 1)),
            child: Row(
              children: [
                Container(
                  width: 36, height: 36,
                  decoration: BoxDecoration(color: transaction.transactionTypeColor.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                  child: Icon(_getTransactionIcon(transaction.transactionType), color: transaction.transactionTypeColor, size: 18),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(transaction.description, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w500), maxLines: 1, overflow: TextOverflow.ellipsis),
                      Text(transaction.formattedShortDate, style: GoogleFonts.poppins(fontSize: 11, color: Colors.grey[500])),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(transaction.formattedAmount, style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: transaction.transactionTypeColor)),
                    if (transaction.hasSaving) Text('+${transaction.formattedSavingAmount}', style: GoogleFonts.poppins(fontSize: 10, color: Colors.green, fontWeight: FontWeight.w500)),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // ✅ [CORRECCIÓN] Se agregó el caso .withdrawal
  IconData _getTransactionIcon(TransactionType type) {
    switch (type) {
      case TransactionType.expense: return Icons.shopping_cart;
      case TransactionType.income: return Icons.attach_money;
      case TransactionType.saving: return Icons.savings;
      case TransactionType.fee: return Icons.receipt;
      case TransactionType.withdrawal: return Icons.arrow_circle_up;
    }
  }
}