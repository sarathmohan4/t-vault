/* eslint-disable no-return-assign */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../styles/GlobalStyles';
import ListItemIcon from '../../assets/icon_safes.svg';
import PopperElement from '../Popper';
import mediaBreakpoints from '../../breakpoints';
import TooltipComponent from '../Tooltip';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  height: 4rem;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
`;
const ListItemDetailBox = styled('div')`
  padding-left: 1.7rem;
`;
const ListItemAvatarWrap = styled.div`
  .MuiAvatar-root {
    border-radius: 0;
  }
  display: flex;
  align-items: center;
`;
const Flag = styled('span')`
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
  color: #a2a1a1;
`;

const FolderIconWrap = styled('div')`
  display: flex;
  justify-content: space-between;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
  }
`;

const PopperItem = styled.div`
  padding: 0.5rem;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background: ${(props) => props.theme.gradients.list || 'none'};
  }
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
`;
const extraCss = css`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 1.6rem;
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

const ListItem = (props) => {
  const {
    title,
    subTitle,
    flag,
    icon,
    showActions,
    popperListItems,
    listIconStyles,
    showToolTip,
  } = props;
  const [anchorEl, setAnchorEl] = useState(null);
  const renderTitle = () => {
    return (
      <TitleOne color="#d0d0d0" extraCss={extraCss}>
        {title}
      </TitleOne>
    );
  };
  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
          <ListItemAvatarWrap>
            <Avatar alt="ListItem_icon" src={icon} classes={listIconStyles} />
          </ListItemAvatarWrap>
          <ListItemDetailBox>
            {showToolTip ? (
              <TooltipComponent
                title={title}
                renderContent={renderTitle()}
                certificate="top"
              />
            ) : (
              <TitleOne extraCss={ListTitleStyles}>
                {title}
                <Flag fontSize="0.85rem" fontStyle="italic">
                  {flag}
                </Flag>
              </TitleOne>
            )}

            <Flag fontSize="1.3rem">{subTitle}</Flag>
          </ListItemDetailBox>
        </LabelWrap>
        {showActions ? (
          <FolderIconWrap>
            <PopperElement
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              anchorEl={anchorEl}
              setAnchorEl={setAnchorEl}
            >
              {popperListItems.map((item) => (
                <PopperItem>
                  {item.icon}
                  <span>{item.title}</span>
                </PopperItem>
              ))}
            </PopperElement>
          </FolderIconWrap>
        ) : (
          <></>
        )}
      </FolderWrap>
    </ComponentError>
  );
};
ListItem.propTypes = {
  subTitle: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.string,
  flag: PropTypes.string,
  showActions: PropTypes.bool.isRequired,
  popperListItems: PropTypes.arrayOf(PropTypes.any),
  listIconStyles: PropTypes.objectOf(PropTypes.any),
  showToolTip: PropTypes.bool,
};
ListItem.defaultProps = {
  subTitle: '',
  title: '',
  flag: '',
  icon: ListItemIcon,
  popperListItems: [],
  listIconStyles: {},
  showToolTip: false,
};
export default ListItem;
