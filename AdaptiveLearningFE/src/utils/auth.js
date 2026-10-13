import Cookies from 'js-cookie'
import { settings } from '@/config/settings'

const { storage, tokenTableName } = settings

//! ----------------------------------------------------------------------------------------------------------

export function getToken() {
  if (storage) {
    if (storage === 'localStorage') {
      return localStorage.getItem(tokenTableName)
    }
    if (storage === 'sessionStorage') {
      return sessionStorage.getItem(tokenTableName)

    } else if (storage === 'cookie') {

      return Cookies.get(tokenTableName)
    }
  } else {
    return localStorage.getItem(tokenTableName)
  }
}

//! ----------------------------------------------------------------------------------------------------------

export function setToken(token) {
  if (storage) {
    if (storage === 'localStorage') {
      return localStorage.setItem(tokenTableName, token)
    }
    if (storage === 'sessionStorage') {
      return sessionStorage.setItem(tokenTableName, token)

    } else if (storage === 'cookie') {

      return Cookies.set(tokenTableName, token)
    }
  } else {
    return localStorage.setItem(tokenTableName, token)
  }
}

//! ----------------------------------------------------------------------------------------------------------

export function removeToken() {
  if (storage) {
    if (storage === 'localStorage') {
      return localStorage.removeItem(tokenTableName)
    }
    if (storage === 'sessionStorage') {
      return sessionStorage.removeItem(tokenTableName)

    } else if (storage === 'cookie') {

      return Cookies.remove(tokenTableName)
    }
  } else {
    return localStorage.removeItem(tokenTableName)
  }
}