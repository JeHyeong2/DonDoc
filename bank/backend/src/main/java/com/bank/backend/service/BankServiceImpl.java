package com.bank.backend.service;

import com.bank.backend.common.utils.EncryptionUtils;
import com.bank.backend.entity.Account;
import com.bank.backend.entity.BankCode;
import com.bank.backend.entity.Owner;
import com.bank.backend.repository.AccountRepository;
import com.bank.backend.repository.BankCodeRepository;
import com.bank.backend.repository.HistoryRepository;
import com.bank.backend.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankServiceImpl implements BankService {

    private final AccountRepository accountRepository;
    private final BankCodeRepository bankCodeRepository;
    private final HistoryRepository historyRepository;
    private final OwnerRepository ownerRepository;

    // 식별번호의 존재 여부만 파악하는 메서드
    @Override
    public boolean certification(String identificationNumber) {
        // 식별 번호 조회
        Optional<Owner> owner = ownerRepository.findByIdentificationNumber(identificationNumber);

        // 존재하는 경우
        if(owner.isPresent()){return false;}

        // 존재하지 않음
        return true;
    }

    // 예금주 생성
    @Override
    @Transactional // read-only => create 가능하도록
    public void createOwner(Map<String, String> info) {
        Owner owner = Owner.builder()
                .ownerName(info.get("ownerName"))
                .identificationNumber(info.get("identificationNumber"))
                .build();
        ownerRepository.save(owner);
    }

    // 계좌 생성
    @Override
    @Transactional
    public void createAccount(Map<String, String> info) {
        // 은행 코드 탐색
        Optional<BankCode> bankCode = bankCodeRepository.findById(Long.parseLong(info.get("bankCode")));

        // 예금주 탐색
        Optional<Owner> owner = ownerRepository.findByIdentificationNumber(info.get("identificationNumber"));

        // 계좌번호 생성
        String accountNumber = EncryptionUtils.makeAccountNumber();

        // 계좌 객체 생성
        Account account = Account.builder()
                .accountNumber(accountNumber) // 계좌번호
                .accountName(info.get("accountName")) // 계좌 이름
                .toCode(bankCode.get()) // 은행 코드
                .password(info.get("password"))// 계좌 비밀번호
                .owner(owner.get()) // 예금주
                .balance(1_000_000) // 잔액
                .salt(info.get("salt")) // 암호화 키
                .build();

        // 계좌 저장
        accountRepository.save(account);
    }

    @Override
    public boolean countAccount(String identification) {
        // 예금주 조회
        Owner owner = ownerRepository.findByIdentificationNumber(identification)
                .orElseThrow(() -> new NoSuchElementException("예금주가 존재하지 않습니다."));
        // 계좌 목록 조회
        List<Account> accountList = accountRepository.findAllByOwner(owner);

        // 계좌 개수 제한
        if(accountList.size() >= 5){
            return false;
        }

        return true;
    }


}
