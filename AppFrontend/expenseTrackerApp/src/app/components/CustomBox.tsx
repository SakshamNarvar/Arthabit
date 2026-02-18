import {StyleSheet, View, ViewStyle, StyleProp} from 'react-native';
import React from 'react';

interface CustomBoxProps {
    style?: any;
    children: React.ReactNode;
    [key: string]: any;
}

// const CustomBox = ({style = {}, children, ...props}) => {
//   return (
//     <View>
//       <Box style={[styles.headingContainer, style.mainBox, style.styles]}>
//         <View style={styles.textColor}>{children}</View>
//       </Box>
//       <Box style={[styles.shadowContainer, style.shadowBox, style.styles]} />
//     </View>
//   );
// };
const CustomBox: React.FC<CustomBoxProps> = ({style = {}, children, ...props}) => {
  // Extract dynamic styles safely
  const mainBoxStyle = style.mainBox || {};
  const shadowBoxStyle = style.shadowBox || {};
  const extraStyles = style.styles || {};

  return (
    <View>
      {/* Combine static styles with dynamic props */}
      <View style={[styles.headingContainer, mainBoxStyle, extraStyles]} {...props}>
        <View style={styles.textColor}>{children}</View>
      </View>
      <View style={[styles.shadowContainer, shadowBoxStyle]} />
    </View>
  );
};

export default CustomBox;

const styles = StyleSheet.create({
  headingContainer: {
    padding: 20,
    borderColor: 'black',
    borderWidth: 1,
    position: 'relative',
    backgroundColor: 'black',
  },
  textColor: {
    // color: 'white',
  },
  shadowContainer: {
    position: 'absolute',
    top: 5,
    left: 5,
    right: -5,
    bottom: -5,
    backgroundColor: 'gray',
    zIndex: -1,
  }
//   ,
//   mainBox: {
//     borderColor: (style) => style.mainBox?.borderColor || 'black',
//     backgroundColor: (style) => style.mainBox?.backgroundColor || 'black',
//   },
//   shadowBox: {
//     backgroundColor: (style) => style.shadowBox?.backgroundColor || 'gray',
//   },
});