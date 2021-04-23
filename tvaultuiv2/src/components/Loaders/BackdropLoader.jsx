import React from 'react';
import PropTypes from 'prop-types';
import Backdrop from '@material-ui/core/Backdrop';
import CircularProgress from '@material-ui/core/CircularProgress';
import { withStyles } from '@material-ui/core/styles';
import theme from '../../theme';

const withStylesProps = (Styles) => (Component) => (props) => {
  const Comp = withStyles(Styles(props, theme))(Component);
  // eslint-disable-next-line react/jsx-props-no-spreading
  return <Comp {...props} />;
};
const Styles = (props) => ({
  backdrop: {
    zIndex: theme.zIndex.modal + 10,
    color: '#fff',
    backgroundColor: 'rgba(0, 0, 0, 0.1)',
    position: props.notAbsolute ? 'none' : 'absolute',
    ...props.muiBackdropOverides,
  },
});
const BackdropLoader = (props) => {
  const { color, classes, notAbsolute } = props;
  return (
    <Backdrop open className={classes.backdrop} notAbsolute={notAbsolute}>
      <CircularProgress color={color} />
    </Backdrop>
  );
};
BackdropLoader.propTypes = {
  color: PropTypes.string,
  classes: PropTypes.objectOf(PropTypes.any),
  notAbsolute: PropTypes.bool,
};
BackdropLoader.defaultProps = {
  color: 'secondary',
  classes: {},
  notAbsolute: false,
};
export default withStylesProps(Styles)(BackdropLoader);
