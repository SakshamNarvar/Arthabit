import React, {useState} from 'react';
import {
  Modal,
  View,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Alert,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  TouchableWithoutFeedback,
  Keyboard,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import CustomText from './CustomText';
import API_CONFIG from '../config/apiConfig';

const CURRENCIES = ['INR', 'USD'] as const;

interface AddExpenseModalProps {
  visible: boolean;
  onClose: () => void;
  onExpenseAdded: () => void;
}

const AddExpenseModal: React.FC<AddExpenseModalProps> = ({
  visible,
  onClose,
  onExpenseAdded,
}) => {
  const [amount, setAmount] = useState('');
  const [merchant, setMerchant] = useState('');
  const [currency, setCurrency] = useState<string>(CURRENCIES[0]);
  const [submitting, setSubmitting] = useState(false);

  const resetForm = () => {
    setAmount('');
    setMerchant('');
    setCurrency(CURRENCIES[0]);
  };

  const handleAddExpense = async () => {
    if (!amount.trim() || isNaN(Number(amount)) || Number(amount) <= 0) {
      Alert.alert('Validation', 'Please enter a valid amount.');
      return;
    }
    if (!merchant.trim()) {
      Alert.alert('Validation', 'Please enter a merchant name.');
      return;
    }

    setSubmitting(true);
    try {
      const accessToken = await AsyncStorage.getItem('accessToken');
      const userId = await AsyncStorage.getItem('userId');

      if (!accessToken || !userId) {
        Alert.alert('Error', 'You are not logged in.');
        setSubmitting(false);
        return;
      }

      const response = await fetch(
        `${API_CONFIG.EXPENSE_SERVICE_URL}/expense/v1/addExpense`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            Authorization: `Bearer ${accessToken}`,
            'X-User-Id': userId,
          },
          body: JSON.stringify({
            amount: parseFloat(amount),
            merchant: merchant.trim(),
            currency: currency,
          }),
        },
      );

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with status ${response.status}`);
      }

      resetForm();
      onClose();
      onExpenseAdded();
    } catch (err: any) {
      console.error('Error adding expense:', err);
      Alert.alert('Error', 'Failed to add expense. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={handleClose}>
      <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
        <View style={styles.overlay}>
          <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            style={styles.keyboardView}>
            <View style={styles.modalContainer}>
              {/* Header */}
              <View style={styles.header}>
                <CustomText style={styles.title}>Add Expense</CustomText>
                <TouchableOpacity onPress={handleClose} style={styles.closeBtn}>
                  <CustomText style={styles.closeBtnText}>✕</CustomText>
                </TouchableOpacity>
              </View>

              {/* Amount */}
              <CustomText style={styles.label}>Amount</CustomText>
              <TextInput
                style={styles.input}
                placeholder="0.00"
                placeholderTextColor="#888"
                keyboardType="decimal-pad"
                value={amount}
                onChangeText={setAmount}
              />

              {/* Merchant */}
              <CustomText style={styles.label}>Merchant Name</CustomText>
              <TextInput
                style={styles.input}
                placeholder="e.g. Amazon, Starbucks"
                placeholderTextColor="#888"
                value={merchant}
                onChangeText={setMerchant}
                autoCapitalize="words"
              />

              {/* Currency Picker */}
              <CustomText style={styles.label}>Currency</CustomText>
              <View style={styles.currencyRow}>
                {CURRENCIES.map(c => (
                  <TouchableOpacity
                    key={c}
                    style={[
                      styles.currencyOption,
                      currency === c && styles.currencyOptionSelected,
                    ]}
                    onPress={() => setCurrency(c)}>
                    <CustomText
                      style={[
                        styles.currencyText,
                        currency === c && styles.currencyTextSelected,
                      ]}>
                      {c}
                    </CustomText>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Submit */}
              <TouchableOpacity
                style={[styles.addButton, submitting && styles.addButtonDisabled]}
                onPress={handleAddExpense}
                disabled={submitting}
                activeOpacity={0.8}>
                {submitting ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <CustomText style={styles.addButtonText}>Add Expense</CustomText>
                )}
              </TouchableOpacity>
            </View>
          </KeyboardAvoidingView>
        </View>
      </TouchableWithoutFeedback>
    </Modal>
  );
};

export default AddExpenseModal;

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  keyboardView: {
    justifyContent: 'flex-end',
  },
  modalContainer: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 36,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#000',
  },
  closeBtn: {
    padding: 4,
  },
  closeBtnText: {
    fontSize: 18,
    color: '#888',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#555',
    marginBottom: 6,
    marginTop: 12,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: '#000',
    backgroundColor: '#F9F9F9',
    fontFamily: 'Helvetica',
  },
  currencyRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 4,
  },
  currencyOption: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#ddd',
    alignItems: 'center',
    backgroundColor: '#F9F9F9',
  },
  currencyOptionSelected: {
    borderColor: '#000',
    backgroundColor: '#000',
  },
  currencyText: {
    fontSize: 15,
    fontWeight: '600',
    color: '#555',
  },
  currencyTextSelected: {
    color: '#fff',
  },
  addButton: {
    marginTop: 24,
    backgroundColor: '#000',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },
  addButtonDisabled: {
    opacity: 0.6,
  },
  addButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});
