import React, {useState, useCallback} from 'react';
import {StyleSheet, View, ScrollView} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import Spends from './Spends';
import Nav from './Nav';
import AddExpenseModal from '../components/AddExpenseModal';

const Home = () => {
  const [modalVisible, setModalVisible] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleExpenseAdded = useCallback(() => {
    setRefreshTrigger(prev => prev + 1);
  }, []);

  return (
      <SafeAreaView style={styles.safeArea}>
        <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
          <View style={styles.container}>
            <Nav />
            <View style={styles.spendsContainer}>
              <Spends
                onAddPress={() => setModalVisible(true)}
                refreshTrigger={refreshTrigger}
              />
            </View>
          </View>
        </ScrollView>
        <AddExpenseModal
          visible={modalVisible}
          onClose={() => setModalVisible(false)}
          onExpenseAdded={handleExpenseAdded}
        />
      </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: 'white',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 40,
  },
  container: {
    flex: 1,
    paddingHorizontal: 16,
    flexDirection: 'column',
  },
  spendsContainer: {
    marginTop: 20,
  },
});

export default Home;