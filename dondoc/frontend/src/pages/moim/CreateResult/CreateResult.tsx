import { useState } from 'react';
import styles from "./CreateResult.module.css";
import { useNavigate, useLocation } from "react-router-dom";
import TermsOfUseModal from './TermsOfUse/TermsOfUse';

interface Props {

}

function CreateResult(props: Props) {

  const navigate = useNavigate()

  const { state } = useLocation()
  const moimName = state.moimName
  const moimInfo = state.moimInfo
  const account = state.account
  const category = state.category

  const [termsOpen, setTermsOpen] = useState<boolean>(false)

  const OpenTerms = () => {
    setTermsOpen(true)
  }
  const CloseTerms = () => {
    setTermsOpen(false)
  }

  const ToBack = () => {
    navigate(-1)
  }

  const ToNext = () => {
    navigate('/moimhome')
  }

  const WatchInfo = () => {
    console.log(state)
  }

  return (
    <div className={styles.container}>
      <div className={styles.content}>

        <div className={styles.topbar}>
          <div className={styles.backbutton}>
            <button className={styles.toback} onClick={ToBack}>
              back
            </button>
          </div>
          <div className={styles.pagename}>
            <h3>모임통장 생성</h3>
          </div>
        </div>

        <div className={styles.createcontent}>
          <div className={styles.createment}>
            <h2>모임 개설준비가 완료 되었습니다</h2>
          </div>

          <div className={styles.accountcontent}>
            <div className={styles.accountinfo}>
              <div className={styles.moimcontent}>
                <p className={styles.title}>모임이름</p>
                <p>{moimName}</p>
              </div>
              <div className={styles.moimcontent}>
                <p className={styles.title}>연결계좌</p>
                <p>{account}</p>
              </div>
              <div className={styles.moimcontent}>
                <p className={styles.title}>계좌유형</p>
                <p>{category.name}</p>
              </div>
              <div className={styles.moiminfo}>
                <p className={styles.title}>모임소개</p>
                <p className={styles.moimtext}>{moimInfo}</p>
              </div>
              <div className={styles.watchtermsbtn}>
                <button className={styles.openterms} onClick={OpenTerms}>
                  약관보기
                </button>
              </div>
              <div className={styles.checkbox}>
                <input type="checkbox" id="scales" name="sclaes" /><label htmlFor="scales">약관에 동의합니다</label>
              </div>
            </div>
          </div>

        <div className={styles.buttondiv}>
            <button className={styles.submitbtn} onClick={ToNext}>계좌 개설하기</button>
        </div>
        
        </div>

        <button onClick={WatchInfo}></button>

        {termsOpen && (
            <>
              <div className={styles.backgroundOverlay} onClick={CloseTerms}/>
              <TermsOfUseModal setTermsOpen={setTermsOpen} />
            </>
          )}

      </div>
    </div>
  );
}

export default CreateResult;