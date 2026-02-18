import React from 'react';
import {Text, StyleSheet, Platform, TextProps, StyleProp, TextStyle} from 'react-native';

interface CustomTextProps extends TextProps {
  style?: StyleProp<TextStyle>;
  children?: React.ReactNode;
}

const CustomText: React.FC<CustomTextProps> = ({style, children, ...props}) => {
  return (
    <Text style={[styles.text, style]} {...props}>
      {children}
    </Text>
  );
};

const styles = StyleSheet.create({
  text: {
    color: 'black',
    fontFamily: 'Helvetica',
  },
});

export default CustomText;