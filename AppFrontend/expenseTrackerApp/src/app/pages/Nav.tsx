import {View, StyleSheet, TouchableOpacity, Image} from 'react-native';
import React from 'react';
import CustomText from '../components/CustomText';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

type RootStackParamList = {
  Profile: undefined;
  // Add other screen params here
};

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'Profile'>;

function Nav(): React.JSX.Element {
  const navigation = useNavigation<NavigationProp>();

  const handleProfilePress = () => {
    navigation.navigate('Profile');
  };

  return (
    <View style={styles.container}>
      <CustomText style={{}}>Logo</CustomText>
      <CustomText style={{}}>Arthabit</CustomText>
      <TouchableOpacity onPress={handleProfilePress}>
        <Image
          source={{
            uri: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHw%3D&auto=format&fit=crop&w=800&q=60',
          }}
          style={styles.avatar}
        />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
  },
});

export default Nav;