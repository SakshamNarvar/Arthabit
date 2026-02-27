import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import Heading from '../components/Heading';
import Expense from '../components/Expense';
import CustomBox from '../components/CustomBox';
import AsyncStorage from '@react-native-async-storage/async-storage';
import CustomText from '../components/CustomText';
import {ExpenseDto} from '../pages/dto/ExpenseDto';
import API_CONFIG from '../config/apiConfig';

interface SpendsProps {
  onAddPress: () => void;
  refreshTrigger?: number;
}

const Spends: React.FC<SpendsProps> = ({ onAddPress, refreshTrigger }) => {
  const [expenses, setExpenses] = useState<ExpenseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchExpenses();
  }, [refreshTrigger]);

  const fetchExpenses = async () => {
    try {
      const SERVER_BASE_URL = API_CONFIG.EXPENSE_SERVICE_URL;
      const accessToken = await AsyncStorage.getItem('accessToken');
      const userId = await AsyncStorage.getItem('userId');

      if (!accessToken || !userId) {
        setExpenses([]);
        setIsLoading(false);
        return;
      }

      const response = await fetch(`${SERVER_BASE_URL}/expense/v1/getExpense`, {
        method: 'GET',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
          'X-User-Id': userId,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch expenses. Status: ${response.status}`);
      }

      const data = await response.json();
      console.log('Expenses fetched:', data);

      if (!Array.isArray(data) || data.length === 0) {
        setExpenses([]);
        setIsLoading(false);
        setError(null);
        return;
      }

      const transformedExpenses: ExpenseDto[] = data.map((expense: any, index: number) => ({
        key: index + 1,
        amount: expense['amount'],
        merchant: expense['merchant'],
        currency: expense['currency'],
        createdAt: new Date(expense['created_at']),
      }));
      console.log('Transformed expenses:', transformedExpenses);

      setExpenses(transformedExpenses);
      setIsLoading(false);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unknown error occurred');
      console.error('Error fetching expenses:', err);
      setIsLoading(false);
    }
  };

  const addExpenseButton = (
    <TouchableOpacity
      style={styles.addButton}
      onPress={onAddPress}
      activeOpacity={0.8}>
      <CustomText style={styles.addButtonText}>+ Add Expense</CustomText>
    </TouchableOpacity>
  );

  if (isLoading) {
    return (
      <View>
        <Heading heading="spends" />
        <CustomBox style={headingBox}>
          <CustomText style={{}}>Loading expenses...</CustomText>
        </CustomBox>
        {addExpenseButton}
      </View>
    );
  }

  if (error) {
    return (
      <View>
        <Heading heading="spends" />
        <CustomBox style={headingBox}>
          <CustomText style={{}}>Error: {error}</CustomText>
        </CustomBox>
        {addExpenseButton}
      </View>
    );
  }

  if (expenses.length === 0) {
    return (
      <View>
        <Heading heading="spends" />
        <CustomBox style={headingBox}>
          <CustomText style={{textAlign: 'center', color: '#888'}}>No expenses found. Start adding some!</CustomText>
        </CustomBox>
        {addExpenseButton}
      </View>
    );
  }

  return (
    <View>
      <Heading heading="spends" />
      <CustomBox style={headingBox}>
        <View style={styles.expenses}>
          {expenses.map(expense => (
            <Expense key={expense.key} props={expense} />
          ))}
        </View>
      </CustomBox>
      {addExpenseButton}
    </View>
  );
};

export default Spends;

const styles = StyleSheet.create({
  expenses: {
    marginTop: 20,
  },
  addButton: {
    marginTop: 16,
    backgroundColor: '#000',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  addButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});

const headingBox = {
  mainBox: {
    backgroundColor: 'white',
    borderColor: 'black',
  },
  shadowBox: {
    backgroundColor: 'gray',
  },
  styles: {
    marginTop: 20,
  },
};