import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import TooltipComponent from '../../../../../components/Tooltip';
import mediaBreakpoints from '../../../../../breakpoints';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
`;
const ListItemDetailBox = styled('div')`
  padding-left: 1.7rem;
  width: 80%;
`;
const ListItemAvatarWrap = styled.div`
  .MuiAvatar-root {
    border-radius: 0;
    width: 5rem;
  }
  display: flex;
  align-items: center;
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
`;
const ListTitleStyles = css`
  color: #d0d0d0;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  width: 25rem;
  ${mediaBreakpoints.belowLarge} {
    width: 17rem;
  }
  ${mediaBreakpoints.medium} {
    width: 12rem;
  }
  ${mediaBreakpoints.small} {
    width: 15rem;
  }
`;

const AzureListItem = (props) => {
  const { title, icon } = props;

  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
          <ListItemAvatarWrap>
            <Avatar alt="ListItem_icon" src={icon} />
          </ListItemAvatarWrap>
          <ListItemDetailBox>
            <TooltipComponent
              title={title}
              renderContent={
                <TitleOne extraCss={ListTitleStyles}>{title}</TitleOne>
              }
              certificate="top"
            />
          </ListItemDetailBox>
        </LabelWrap>
      </FolderWrap>
    </ComponentError>
  );
};
AzureListItem.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string.isRequired,
};
AzureListItem.defaultProps = {
  title: '',
};
export default AzureListItem;
